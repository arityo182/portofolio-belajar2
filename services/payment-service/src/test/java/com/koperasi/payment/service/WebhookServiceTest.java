package com.koperasi.payment.service;

import com.koperasi.payment.domain.dto.InstallmentInfo;
import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.domain.entity.WebhookEvent;
import com.koperasi.payment.domain.enums.DisbursementStatus;
import com.koperasi.payment.domain.enums.PaymentGateway;
import com.koperasi.payment.domain.enums.PaymentStatus;
import com.koperasi.payment.event.PaymentEventPublisher;
import com.koperasi.payment.event.PaymentEventType;
import com.koperasi.payment.event.WebhookResult;
import com.koperasi.payment.exception.InvalidWebhookSignatureException;
import com.koperasi.payment.gateway.MidtransClient;
import com.koperasi.payment.gateway.XenditClient;
import com.koperasi.payment.repository.LoanDisbursementRepository;
import com.koperasi.payment.repository.PaymentRepository;
import com.koperasi.payment.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LoanDisbursementRepository loanDisbursementRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private MidtransClient midtransClient;

    @Mock
    private XenditClient xenditClient;

    @Mock
    private LoanServiceClient loanServiceClient;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private WebhookService webhookService;
    private Payment payment;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(
                paymentRepository,
                loanDisbursementRepository,
                webhookEventRepository,
                midtransClient,
                xenditClient,
                loanServiceClient,
                paymentEventPublisher
        );
        payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1375000.00"), PaymentGateway.MIDTRANS, "idem-1");
        lenient().when(webhookEventRepository.save(any(WebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void handleMidtrans_withSettlement_marksPaymentSuccessAndPublishesEvent() {
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.findByEventKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(loanServiceClient.getInstallment(payment.getLoanInstallmentId()))
                .thenReturn(installment(payment));

        WebhookResult result = webhookService.handleMidtrans(
                payload(payment.getId(), "settlement", "signature"));

        assertThat(result.processed()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(loanServiceClient).markInstallmentPaid(eq(payment.getLoanInstallmentId()), any());
        verify(paymentEventPublisher).publishAfterCommit(
                eq(PaymentEventType.INSTALLMENT_RECEIVED), eq(payment.getId().toString()), any());
    }

    @Test
    void handleMidtrans_withInvalidSignature_throwsBadRequestException() {
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> webhookService.handleMidtrans(
                payload(payment.getId(), "settlement", "bad")))
                .isInstanceOf(InvalidWebhookSignatureException.class);

        verifyNoInteractions(paymentRepository, webhookEventRepository);
    }

    @Test
    void handleMidtrans_whenEventAlreadyProcessed_returnsProcessedWithoutUpdatingPayment() {
        WebhookEvent event = WebhookEvent.create("event", PaymentGateway.MIDTRANS, "txn", "settlement",
                Map.of(), true);
        event.markProcessed();
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.findByEventKey(anyString())).thenReturn(Optional.of(event));

        WebhookResult result = webhookService.handleMidtrans(payload(payment.getId(), "settlement", "signature"));

        assertThat(result.processed()).isTrue();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void handleMidtrans_withPendingStatus_updatesExternalIdWithoutPublishing() {
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.findByEventKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        webhookService.handleMidtrans(payload(payment.getId(), "pending", "signature"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getExternalTransactionId()).isNotBlank();
        verify(paymentEventPublisher, never()).publishAfterCommit(anyString(), anyString(), any());
    }

    @Test
    void handleXendit_withPaidStatus_marksPaymentSuccess() {
        Payment xenditPayment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1375000.00"), PaymentGateway.XENDIT, "idem-x");
        when(xenditClient.verifyCallbackToken("token")).thenReturn(true);
        when(webhookEventRepository.findByEventKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(xenditPayment.getId())).thenReturn(Optional.of(xenditPayment));
        when(paymentRepository.save(xenditPayment)).thenReturn(xenditPayment);
        when(loanServiceClient.getInstallment(xenditPayment.getLoanInstallmentId()))
                .thenReturn(installment(xenditPayment));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", "XEN-TXN-1");
        payload.put("external_id", xenditPayment.getId().toString());
        payload.put("status", "PAID");
        payload.put("amount", new BigDecimal("1375000.00"));

        WebhookResult result = webhookService.handleXendit("token", payload);

        assertThat(result.processed()).isTrue();
        assertThat(xenditPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void handleXendit_withInvalidToken_throwsBadRequestException() {
        when(xenditClient.verifyCallbackToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> webhookService.handleXendit("bad", Map.of()))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void handleMidtrans_disbursementSettlement_marksDisbursementCompleted() {
        LoanDisbursement disbursement = LoanDisbursement.create(UUID.randomUUID(),
                new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS, "disb-1");
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.findByEventKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(disbursement.getId())).thenReturn(Optional.empty());
        when(paymentRepository.findByExternalTransactionId(anyString())).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findById(disbursement.getId())).thenReturn(Optional.of(disbursement));
        when(loanDisbursementRepository.save(disbursement)).thenReturn(disbursement);

        WebhookResult result = webhookService.handleMidtrans(
                payload(disbursement.getId(), "settlement", "signature"));

        assertThat(result.processed()).isTrue();
        assertThat(disbursement.getStatus()).isEqualTo(DisbursementStatus.COMPLETED);
        verify(paymentEventPublisher).publishAfterCommit(
                eq(PaymentEventType.DISBURSEMENT_COMPLETED), eq(disbursement.getId().toString()), any());
    }

    @Test
    void handleMidtrans_whenPaymentUnknown_returnsNotProcessed() {
        UUID unknownId = UUID.randomUUID();
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.findByEventKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(unknownId)).thenReturn(Optional.empty());
        when(paymentRepository.findByExternalTransactionId(anyString())).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findById(unknownId)).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findByLoanApplicationId(unknownId)).thenReturn(Optional.empty());
        when(loanDisbursementRepository.findByExternalTransactionId(anyString())).thenReturn(Optional.empty());

        WebhookResult result = webhookService.handleMidtrans(payload(unknownId, "settlement", "signature"));

        assertThat(result.processed()).isFalse();
    }

    private Map<String, Object> payload(UUID orderId, String status, String signature) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("order_id", orderId.toString());
        payload.put("transaction_id", "MID-TXN-1");
        payload.put("transaction_status", status);
        payload.put("status_code", "200");
        payload.put("gross_amount", "1375000.00");
        payload.put("signature_key", signature);
        return payload;
    }

    private InstallmentInfo installment(Payment payment) {
        return new InstallmentInfo(
                payment.getLoanInstallmentId(),
                UUID.randomUUID(),
                payment.getBorrowerId(),
                1,
                LocalDate.now().plusMonths(1),
                payment.getAmount(),
                "PENDING",
                null
        );
    }
}
