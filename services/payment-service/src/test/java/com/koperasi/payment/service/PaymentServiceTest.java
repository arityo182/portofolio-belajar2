package com.koperasi.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.domain.dto.CreateDisbursementRequest;
import com.koperasi.payment.domain.dto.CreatePaymentRequest;
import com.koperasi.payment.domain.dto.DisbursementResponse;
import com.koperasi.payment.domain.dto.InstallmentInfo;
import com.koperasi.payment.domain.dto.LoanApplicationInfo;
import com.koperasi.payment.domain.dto.PaymentResponse;
import com.koperasi.payment.domain.dto.PaymentStatusResponse;
import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.domain.enums.DisbursementStatus;
import com.koperasi.payment.domain.enums.PaymentGateway;
import com.koperasi.payment.domain.enums.PaymentStatus;
import com.koperasi.payment.event.PaymentEventPublisher;
import com.koperasi.payment.event.PaymentEventType;
import com.koperasi.payment.event.PaymentRetryPublisher;
import com.koperasi.payment.exception.BusinessValidationException;
import com.koperasi.payment.exception.DistributedLockException;
import com.koperasi.payment.exception.ForbiddenOperationException;
import com.koperasi.payment.exception.PaymentAlreadyCompletedException;
import com.koperasi.payment.exception.PaymentGatewayException;
import com.koperasi.payment.exception.PaymentNotFoundException;
import com.koperasi.payment.gateway.DisbursementResult;
import com.koperasi.payment.gateway.MidtransClient;
import com.koperasi.payment.gateway.PaymentLinkResult;
import com.koperasi.payment.gateway.XenditClient;
import com.koperasi.payment.repository.LoanDisbursementRepository;
import com.koperasi.payment.repository.PaymentRepository;
import com.koperasi.payment.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final BigDecimal AMOUNT = new BigDecimal("1375000.00");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LoanDisbursementRepository loanDisbursementRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private LoanServiceClient loanServiceClient;

    @Mock
    private MidtransClient midtransClient;

    @Mock
    private XenditClient xenditClient;

    @Mock
    private PaymentRetryPublisher paymentRetryPublisher;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private PaymentProperties properties;
    private PaymentServiceImpl paymentService;
    private JwtPrincipal borrower;
    private JwtPrincipal otherBorrower;
    private JwtPrincipal reviewer;
    private UUID installmentId;
    private UUID loanApplicationId;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.setIdempotencyTtlHours(24);
        properties.setLockTtlSeconds(15);
        properties.setRetryMaxAttempts(3);
        properties.setRetryBaseBackoffMs(1000);
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                loanDisbursementRepository,
                idempotencyService,
                distributedLockService,
                loanServiceClient,
                midtransClient,
                xenditClient,
                paymentRetryPublisher,
                paymentEventPublisher,
                properties,
                new ObjectMapper().findAndRegisterModules()
        );
        borrower = principal("BORROWER");
        otherBorrower = principal("BORROWER");
        reviewer = principal("REVIEWER");
        installmentId = UUID.randomUUID();
        loanApplicationId = UUID.randomUUID();
    }

    @Test
    void payInstallment_withValidRequest_createsPendingPaymentAndCachesResponse() {
        InstallmentInfo installment = installment(borrower.userId());
        when(idempotencyService.findCached("idem-1")).thenReturn(Optional.empty());
        when(loanServiceClient.getInstallment(installmentId)).thenReturn(installment);
        when(idempotencyService.start("idem-1", "payment")).thenReturn("lease");
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock");
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(midtransClient.createPaymentLink(any(Payment.class)))
                .thenReturn(new PaymentLinkResult("https://pay.test/1", "MID-TXN-1"));

        PaymentResponse response = paymentService.payInstallment(
                installmentId,
                new CreatePaymentRequest(PaymentGateway.MIDTRANS, AMOUNT),
                "idem-1",
                borrower
        );

        assertThat(response.paymentId()).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.paymentUrl()).isEqualTo("https://pay.test/1");
        assertThat(response.externalTransactionId()).isEqualTo("MID-TXN-1");
        verify(midtransClient).createPaymentLink(any(Payment.class));
        verify(idempotencyService).complete(eq("idem-1"), eq("lease"), any(PaymentResponse.class));
        verify(distributedLockService).release(anyString(), eq("lock"));
    }

    @Test
    void payInstallment_withCachedIdempotencyKey_returnsCachedResponseWithoutGatewayCall() {
        PaymentResponse cached = new PaymentResponse(
                UUID.randomUUID(), installmentId, PaymentStatus.PENDING,
                PaymentGateway.MIDTRANS, "https://cached", "MID-TXN-CACHED"
        );
        when(idempotencyService.findCached("idem-1")).thenReturn(Optional.of(Map.of(
                "paymentId", cached.paymentId().toString(),
                "installmentId", cached.installmentId().toString(),
                "status", "PENDING",
                "paymentGateway", "MIDTRANS",
                "paymentUrl", cached.paymentUrl(),
                "externalTransactionId", cached.externalTransactionId()
        )));

        PaymentResponse response = paymentService.payInstallment(
                installmentId,
                new CreatePaymentRequest(PaymentGateway.MIDTRANS, AMOUNT),
                "idem-1",
                borrower
        );

        assertThat(response.paymentId()).isEqualTo(cached.paymentId());
        verifyNoInteractions(loanServiceClient, midtransClient, xenditClient, paymentRepository);
    }

    @Test
    void payInstallment_withMismatchedAmount_throwsBusinessValidation() {
        when(idempotencyService.findCached("idem-1")).thenReturn(Optional.empty());
        when(loanServiceClient.getInstallment(installmentId)).thenReturn(installment(borrower.userId()));

        assertThatThrownBy(() -> paymentService.payInstallment(
                installmentId,
                new CreatePaymentRequest(PaymentGateway.MIDTRANS, new BigDecimal("1.00")),
                "idem-1",
                borrower
        )).isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void payInstallment_forAnotherBorrower_throwsForbidden() {
        when(idempotencyService.findCached("idem-1")).thenReturn(Optional.empty());
        when(loanServiceClient.getInstallment(installmentId)).thenReturn(installment(otherBorrower.userId()));

        assertThatThrownBy(() -> paymentService.payInstallment(
                installmentId,
                new CreatePaymentRequest(PaymentGateway.MIDTRANS, AMOUNT),
                "idem-1",
                borrower
        )).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void payInstallment_whenInstallmentAlreadyPaid_throwsAlreadyCompleted() {
        InstallmentInfo paid = new InstallmentInfo(
                installmentId, loanApplicationId, borrower.userId(), 1, LocalDate.now(),
                AMOUNT, "PAID", Instant.now());
        when(idempotencyService.findCached("idem-1")).thenReturn(Optional.empty());
        when(loanServiceClient.getInstallment(installmentId)).thenReturn(paid);

        assertThatThrownBy(() -> paymentService.payInstallment(
                installmentId,
                new CreatePaymentRequest(PaymentGateway.MIDTRANS, AMOUNT),
                "idem-1",
                borrower
        )).isInstanceOf(PaymentAlreadyCompletedException.class);
    }

    @Test
    void payInstallment_whenPaymentLockHeld_throwsDistributedLockException() {
        when(idempotencyService.findCached("idem-1")).thenReturn(Optional.empty());
        when(loanServiceClient.getInstallment(installmentId)).thenReturn(installment(borrower.userId()));
        when(idempotencyService.start("idem-1", "payment")).thenReturn("lease");
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn(null);

        assertThatThrownBy(() -> paymentService.payInstallment(
                installmentId,
                new CreatePaymentRequest(PaymentGateway.MIDTRANS, AMOUNT),
                "idem-1",
                borrower
        )).isInstanceOf(DistributedLockException.class);

        verify(idempotencyService).fail("idem-1", "lease");
        verifyNoInteractions(midtransClient, xenditClient);
    }

    @Test
    void payInstallment_whenGatewayFails_marksFailedAndEnqueuesRetry() {
        InstallmentInfo installment = installment(borrower.userId());
        when(idempotencyService.findCached("idem-1")).thenReturn(Optional.empty());
        when(loanServiceClient.getInstallment(installmentId)).thenReturn(installment);
        when(idempotencyService.start("idem-1", "payment")).thenReturn("lease");
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock");
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(midtransClient.createPaymentLink(any(Payment.class)))
                .thenThrow(new PaymentGatewayException("gateway down"));

        assertThatThrownBy(() -> paymentService.payInstallment(
                installmentId,
                new CreatePaymentRequest(PaymentGateway.MIDTRANS, AMOUNT),
                "idem-1",
                borrower
        )).isInstanceOf(PaymentGatewayException.class);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, org.mockito.Mockito.atLeast(2)).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRetryPublisher).enqueueFailedPaymentAfterCommit(any(), eq(installmentId), anyString());
        verify(idempotencyService).fail("idem-1", "lease");
        verify(paymentEventPublisher).publishAfterCommit(
                eq(PaymentEventType.INSTALLMENT_FAILED), anyString(), any());
    }

    @Test
    void initiateDisbursement_withValidRequest_createsPendingDisbursement() {
        when(loanServiceClient.getLoanApplication(loanApplicationId)).thenReturn(approvedLoan());
        when(idempotencyService.findCached("disb-1")).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findByLoanApplicationId(loanApplicationId)).thenReturn(Optional.empty());
        when(idempotencyService.start("disb-1", "disbursement")).thenReturn("lease");
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock");
        when(loanDisbursementRepository.saveAndFlush(any(LoanDisbursement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(midtransClient.initiateDisbursement(any(LoanDisbursement.class)))
                .thenReturn(new DisbursementResult("MID-DISB-1", "PENDING"));

        DisbursementResponse response = paymentService.initiateDisbursement(
                new CreateDisbursementRequest(loanApplicationId, new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS),
                "disb-1",
                reviewer
        );

        assertThat(response.disbursementId()).isNotNull();
        assertThat(response.status()).isEqualTo(DisbursementStatus.PENDING);
        assertThat(response.externalTransactionId()).isEqualTo("MID-DISB-1");
        verify(paymentEventPublisher).publishAfterCommit(
                eq(PaymentEventType.DISBURSEMENT_INITIATED), anyString(), any());
        verify(idempotencyService).complete(eq("disb-1"), eq("lease"), any(DisbursementResponse.class));
    }

    @Test
    void initiateDisbursement_whenExistingCompleted_returnsExistingWithoutGatewayCall() {
        LoanDisbursement existing = LoanDisbursement.create(
                loanApplicationId, new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS, "old-key");
        existing.setStatus(DisbursementStatus.COMPLETED);
        when(idempotencyService.findCached("disb-2")).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findByLoanApplicationId(loanApplicationId)).thenReturn(Optional.of(existing));

        DisbursementResponse response = paymentService.initiateDisbursement(
                new CreateDisbursementRequest(loanApplicationId, new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS),
                "disb-2",
                reviewer
        );

        assertThat(response.disbursementId()).isEqualTo(existing.getId());
        verifyNoInteractions(midtransClient, xenditClient);
        verify(idempotencyService).findCached("disb-2");
        verify(idempotencyService, never()).start(anyString(), anyString());
    }

    @Test
    void initiateDisbursement_whenExistingInProgress_throwsConflict() {
        LoanDisbursement existing = LoanDisbursement.create(
                loanApplicationId, new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS, "old-key");
        existing.setStatus(DisbursementStatus.PENDING);
        when(idempotencyService.findCached("disb-2")).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findByLoanApplicationId(loanApplicationId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.initiateDisbursement(
                new CreateDisbursementRequest(loanApplicationId, new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS),
                "disb-2",
                reviewer
        )).isInstanceOf(DistributedLockException.class);
    }

    @Test
    void initiateDisbursement_whenLockHeld_throwsAndFailsIdempotency() {
        when(loanServiceClient.getLoanApplication(loanApplicationId)).thenReturn(approvedLoan());
        when(idempotencyService.findCached("disb-1")).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findByLoanApplicationId(loanApplicationId)).thenReturn(Optional.empty());
        when(idempotencyService.start("disb-1", "disbursement")).thenReturn("lease");
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn(null);

        assertThatThrownBy(() -> paymentService.initiateDisbursement(
                new CreateDisbursementRequest(loanApplicationId, new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS),
                "disb-1",
                reviewer
        )).isInstanceOf(DistributedLockException.class);

        verify(idempotencyService).fail("disb-1", "lease");
    }

    @Test
    void retryPayment_whenGatewaySucceeds_updatesPaymentAndCachesResponse() {
        Payment payment = Payment.create(
                installmentId, borrower.userId(), AMOUNT, PaymentGateway.MIDTRANS, "idem-1");
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(payment)).thenReturn(payment);
        when(midtransClient.createPaymentLink(payment))
                .thenReturn(new PaymentLinkResult("https://retry.test", "MID-RETRY"));

        paymentService.retryPayment(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentUrl()).isEqualTo("https://retry.test");
        verify(idempotencyService).complete(eq("idem-1"), eq(null), any(PaymentResponse.class));
    }

    @Test
    void retryPayment_whenGatewayFails_leavesFailed() {
        Payment payment = Payment.create(
                installmentId, borrower.userId(), AMOUNT, PaymentGateway.MIDTRANS, "idem-1");
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(payment)).thenReturn(payment);
        when(midtransClient.createPaymentLink(payment)).thenThrow(new PaymentGatewayException("down"));

        assertThatThrownBy(() -> paymentService.retryPayment(payment.getId()))
                .isInstanceOf(PaymentGatewayException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(idempotencyService, never()).complete(anyString(), any(), any());
    }

    @Test
    void getInstallmentStatus_asOwner_returnsStatus() {
        Payment payment = Payment.create(
                installmentId, borrower.userId(), AMOUNT, PaymentGateway.MIDTRANS, "idem-1");
        when(paymentRepository.findFirstByLoanInstallmentIdOrderByCreatedAtDesc(installmentId))
                .thenReturn(Optional.of(payment));

        PaymentStatusResponse response = paymentService.getInstallmentStatus(installmentId, borrower);

        assertThat(response.paymentStatus()).isEqualTo("PENDING");
        assertThat(response.installmentStatus()).isEqualTo("PENDING");
    }

    @Test
    void getInstallmentStatus_asOtherBorrower_throwsForbidden() {
        Payment payment = Payment.create(
                installmentId, borrower.userId(), AMOUNT, PaymentGateway.MIDTRANS, "idem-1");
        when(paymentRepository.findFirstByLoanInstallmentIdOrderByCreatedAtDesc(installmentId))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getInstallmentStatus(installmentId, otherBorrower))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void getInstallmentStatus_whenMissing_throwsPaymentNotFound() {
        when(paymentRepository.findFirstByLoanInstallmentIdOrderByCreatedAtDesc(installmentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getInstallmentStatus(installmentId, borrower))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    private InstallmentInfo installment(UUID borrowerId) {
        return new InstallmentInfo(
                installmentId,
                loanApplicationId,
                borrowerId,
                1,
                LocalDate.now().plusMonths(1),
                AMOUNT,
                "PENDING",
                null
        );
    }

    private JwtPrincipal principal(String role) {
        UUID id = UUID.randomUUID();
        return new JwtPrincipal(id, id + "@example.id", Set.of(role), "jti-" + id,
                Instant.now().plusSeconds(900));
    }

    private LoanApplicationInfo approvedLoan() {
        return new LoanApplicationInfo(
                loanApplicationId, reviewer.userId(), new BigDecimal("10000000.00"), "APPROVED");
    }

    @Test
    void initiateDisbursement_withMismatchedAmount_isRejected() {
        when(loanServiceClient.getLoanApplication(loanApplicationId)).thenReturn(approvedLoan());

        assertThatThrownBy(() -> paymentService.initiateDisbursement(
                new CreateDisbursementRequest(loanApplicationId, new BigDecimal("9999999.00"), PaymentGateway.MIDTRANS),
                "disb-mismatch",
                reviewer
        )).isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Nominal pencairan");
    }

    @Test
    void initiateDisbursement_whenLoanNotApproved_isRejected() {
        when(loanServiceClient.getLoanApplication(loanApplicationId)).thenReturn(
                new LoanApplicationInfo(loanApplicationId, reviewer.userId(), new BigDecimal("10000000.00"), "UNDER_REVIEW"));

        assertThatThrownBy(() -> paymentService.initiateDisbursement(
                new CreateDisbursementRequest(loanApplicationId, new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS),
                "disb-status",
                reviewer
        )).isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("APPROVED");
    }
}
