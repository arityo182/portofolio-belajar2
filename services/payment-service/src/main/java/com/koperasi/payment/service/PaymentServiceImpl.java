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
import com.koperasi.payment.exception.IdempotencyConflictException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 100;

    private final PaymentRepository paymentRepository;
    private final LoanDisbursementRepository loanDisbursementRepository;
    private final IdempotencyService idempotencyService;
    private final DistributedLockService distributedLockService;
    private final LoanServiceClient loanServiceClient;
    private final MidtransClient midtransClient;
    private final XenditClient xenditClient;
    private final PaymentRetryPublisher paymentRetryPublisher;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(noRollbackFor = PaymentGatewayException.class)
    public PaymentResponse payInstallment(UUID installmentId, CreatePaymentRequest request,
                                          String idempotencyKey, JwtPrincipal principal) {
        validateIdempotencyKey(idempotencyKey);
        Optional<Map<String, Object>> cached = idempotencyService.findCached(idempotencyKey);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), PaymentResponse.class);
        }

        InstallmentInfo installment = loanServiceClient.getInstallment(installmentId);
        if (!installment.borrowerId().equals(principal.userId())) {
            throw new ForbiddenOperationException("Anda tidak memiliki akses ke cicilan ini");
        }
        if (installment.isPaid() || paymentRepository.existsByLoanInstallmentIdAndStatus(
                installmentId, PaymentStatus.SUCCESS)) {
            throw new PaymentAlreadyCompletedException();
        }
        if (installment.amount() == null || request.amount().compareTo(installment.amount()) != 0) {
            throw new BusinessValidationException("Nominal pembayaran tidak sesuai dengan cicilan");
        }

        String lease = idempotencyService.start(idempotencyKey, "payment");
        String lockKey = "lock:pay:" + installmentId;
        String lockToken = distributedLockService.tryAcquire(lockKey, paymentProperties.lockTtl());
        if (lockToken == null) {
            idempotencyService.fail(idempotencyKey, lease);
            throw new DistributedLockException("Pembayaran cicilan sedang diproses");
        }
        boolean releaseAfterCommit = scheduleLockRelease(lockKey, lockToken);

        try {
            java.util.Optional<Payment> existingPayment =
                    paymentRepository.findFirstByLoanInstallmentIdOrderByCreatedAtDesc(installmentId);
            if (existingPayment.isPresent()) {
                if (existingPayment.get().getStatus() == PaymentStatus.SUCCESS) {
                    throw new PaymentAlreadyCompletedException();
                }
                if (existingPayment.get().getStatus() == PaymentStatus.PENDING) {
                    throw new IdempotencyConflictException("Pembayaran cicilan sedang diproses");
                }
            }
            Payment payment = Payment.create(
                    installmentId,
                    principal.userId(),
                    request.amount(),
                    request.paymentGateway(),
                    idempotencyKey
            );
            payment = paymentRepository.saveAndFlush(payment);

            try {
                PaymentLinkResult link = createPaymentLink(payment);
                payment.setPaymentUrl(link.paymentUrl());
                payment.setExternalTransactionId(link.externalTransactionId());
                payment.setStatus(PaymentStatus.PENDING);
                payment = paymentRepository.saveAndFlush(payment);
            } catch (PaymentGatewayException ex) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.saveAndFlush(payment);
                enqueueRetry(payment, installment, ex);
                publishInstallmentFailed(payment, installment, ex.getMessage());
                idempotencyService.fail(idempotencyKey, lease);
                throw ex;
            }

            PaymentResponse response = PaymentResponse.from(payment);
            idempotencyService.complete(idempotencyKey, lease, response);
            log.info("Pembayaran diinisiasi paymentId={} installmentId={} gateway={}",
                    payment.getId(), installmentId, payment.getPaymentGateway());
            return response;
        } catch (RuntimeException ex) {
            if (!(ex instanceof PaymentGatewayException)) {
                idempotencyService.fail(idempotencyKey, lease);
            }
            throw ex;
        } finally {
            if (!releaseAfterCommit) {
                releaseLock(lockKey, lockToken);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getInstallmentStatus(UUID installmentId, JwtPrincipal principal) {
        Payment payment = paymentRepository.findFirstByLoanInstallmentIdOrderByCreatedAtDesc(installmentId)
                .orElseThrow(() -> new PaymentNotFoundException(installmentId));
        if (!isReviewer(principal) && !payment.getBorrowerId().equals(principal.userId())) {
            throw new ForbiddenOperationException("Anda tidak memiliki akses ke pembayaran ini");
        }
        return PaymentStatusResponse.from(payment);
    }

    @Override
    @Transactional(noRollbackFor = PaymentGatewayException.class)
    public DisbursementResponse initiateDisbursement(CreateDisbursementRequest request,
                                                      String idempotencyKey, JwtPrincipal principal) {
        validateIdempotencyKey(idempotencyKey);
        Optional<Map<String, Object>> cached = idempotencyService.findCached(idempotencyKey);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), DisbursementResponse.class);
        }

        LoanDisbursement existing = loanDisbursementRepository
                .findByLoanApplicationId(request.loanApplicationId())
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == DisbursementStatus.COMPLETED) {
                return DisbursementResponse.from(existing);
            }
            throw new DistributedLockException("Pencairan untuk pinjaman ini sedang diproses atau sudah gagal");
        }

        LoanApplicationInfo loan = loanServiceClient.getLoanApplication(request.loanApplicationId());
        if (!"APPROVED".equalsIgnoreCase(loan.status())) {
            throw new BusinessValidationException("Pencairan hanya dapat dilakukan untuk pinjaman berstatus APPROVED");
        }
        if (loan.amount() == null || request.amount().compareTo(loan.amount()) != 0) {
            throw new BusinessValidationException("Nominal pencairan tidak sesuai dengan nilai pinjaman");
        }

        String lease = idempotencyService.start(idempotencyKey, "disbursement");
        String lockKey = "lock:disburse:" + request.loanApplicationId();
        String lockToken = distributedLockService.tryAcquire(lockKey, paymentProperties.lockTtl());
        if (lockToken == null) {
            idempotencyService.fail(idempotencyKey, lease);
            throw new DistributedLockException("Pencairan untuk pinjaman ini sedang diproses");
        }
        boolean releaseAfterCommit = scheduleLockRelease(lockKey, lockToken);

        try {
            LoanDisbursement disbursement = LoanDisbursement.create(
                    request.loanApplicationId(),
                    request.amount(),
                    request.paymentGateway(),
                    idempotencyKey
            );
            disbursement = loanDisbursementRepository.saveAndFlush(disbursement);

            try {
                DisbursementResult result = initiateGatewayDisbursement(disbursement);
                disbursement.setExternalTransactionId(result.externalTransactionId());
                disbursement.setStatus(DisbursementStatus.PENDING);
                disbursement = loanDisbursementRepository.saveAndFlush(disbursement);
            } catch (PaymentGatewayException ex) {
                disbursement.setStatus(DisbursementStatus.FAILED);
                disbursement = loanDisbursementRepository.saveAndFlush(disbursement);
                publishDisbursement(disbursement, PaymentEventType.DISBURSEMENT_FAILED, ex.getMessage());
                idempotencyService.fail(idempotencyKey, lease);
                throw ex;
            }

            DisbursementResponse response = DisbursementResponse.from(disbursement);
            idempotencyService.complete(idempotencyKey, lease, response);
            publishDisbursement(disbursement, PaymentEventType.DISBURSEMENT_INITIATED, null);
            log.info("Pencairan diinisiasi disbursementId={} loanApplicationId={}",
                    disbursement.getId(), disbursement.getLoanApplicationId());
            return response;
        } catch (RuntimeException ex) {
            if (!(ex instanceof PaymentGatewayException)) {
                idempotencyService.fail(idempotencyKey, lease);
            }
            throw ex;
        } finally {
            if (!releaseAfterCommit) {
                releaseLock(lockKey, lockToken);
            }
        }
    }

    @Override
    @Transactional(noRollbackFor = PaymentGatewayException.class)
    public void retryPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }
        try {
            PaymentLinkResult link = createPaymentLink(payment);
            payment.setPaymentUrl(link.paymentUrl());
            payment.setExternalTransactionId(link.externalTransactionId());
            payment.setStatus(PaymentStatus.PENDING);
            payment = paymentRepository.saveAndFlush(payment);
            idempotencyService.complete(payment.getIdempotencyKey(), null, PaymentResponse.from(payment));
            log.info("Retry pembayaran berhasil paymentId={}", payment.getId());
        } catch (PaymentGatewayException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.saveAndFlush(payment);
            throw ex;
        }
    }

    private PaymentLinkResult createPaymentLink(Payment payment) {
        if (payment.getPaymentGateway() == PaymentGateway.MIDTRANS) {
            return midtransClient.createPaymentLink(payment);
        }
        return xenditClient.createPaymentLink(payment);
    }

    private DisbursementResult initiateGatewayDisbursement(LoanDisbursement disbursement) {
        if (disbursement.getPaymentGateway() == PaymentGateway.MIDTRANS) {
            return midtransClient.initiateDisbursement(disbursement);
        }
        return xenditClient.initiateDisbursement(disbursement);
    }

    private void enqueueRetry(Payment payment, InstallmentInfo installment, PaymentGatewayException ex) {
        try {
            paymentRetryPublisher.enqueueFailedPaymentAfterCommit(
                    payment.getId(), installment.installmentId(), ex.getMessage());
        } catch (RuntimeException rabbitEx) {
            log.error("Gagal memasukkan pembayaran ke retry queue paymentId={}", payment.getId(), rabbitEx);
        }
    }

    private void publishInstallmentFailed(Payment payment, InstallmentInfo installment, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("installmentId", payment.getLoanInstallmentId().toString());
        payload.put("loanApplicationId", installment.loanApplicationId().toString());
        payload.put("borrowerId", payment.getBorrowerId().toString());
        payload.put("amount", payment.getAmount());
        payload.put("failureReason", reason);
        payload.put("retryCount", 1);
        payload.put("failedAt", Instant.now().toString());
        paymentEventPublisher.publishAfterCommit(PaymentEventType.INSTALLMENT_FAILED,
                payment.getId().toString(), payload);
    }

    private void publishDisbursement(LoanDisbursement disbursement, String eventType, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("disbursementId", disbursement.getId().toString());
        payload.put("loanApplicationId", disbursement.getLoanApplicationId().toString());
        payload.put("amount", disbursement.getAmount());
        payload.put("paymentGateway", disbursement.getPaymentGateway().name());
        payload.put("idempotencyKey", disbursement.getIdempotencyKey());
        if (PaymentEventType.DISBURSEMENT_INITIATED.equals(eventType)) {
            payload.put("initiatedAt", Instant.now().toString());
        } else {
            payload.put("failureReason", failureReason);
            payload.put("failedAt", Instant.now().toString());
        }
        paymentEventPublisher.publishAfterCommit(eventType, disbursement.getId().toString(), payload);
    }

    private boolean scheduleLockRelease(String lockKey, String lockToken) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseLock(lockKey, lockToken);
            }
        });
        return true;
    }

    private void releaseLock(String lockKey, String lockToken) {
        try {
            distributedLockService.release(lockKey, lockToken);
        } catch (RuntimeException ex) {
            log.warn("Gagal melepaskan lock {}: {}", lockKey, ex.getMessage());
        }
    }

    private boolean isReviewer(JwtPrincipal principal) {
        return principal.roles() != null
                && principal.roles().stream().anyMatch(Set.of("REVIEWER", "ADMIN")::contains);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessValidationException("Header Idempotency-Key wajib diisi");
        }
        if (idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BusinessValidationException("Idempotency-Key maksimal " + IDEMPOTENCY_KEY_MAX_LENGTH + " karakter");
        }
    }
}
