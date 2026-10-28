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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final LoanDisbursementRepository loanDisbursementRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final MidtransClient midtransClient;
    private final XenditClient xenditClient;
    private final LoanServiceClient loanServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public WebhookResult handleMidtrans(Map<String, Object> payload) {
        String orderId = stringValue(payload.get("order_id"));
        String statusCode = stringValue(payload.get("status_code"));
        String grossAmount = stringValue(payload.get("gross_amount"));
        String signature = stringValue(payload.get("signature_key"));
        if (!midtransClient.verifySignature(orderId, statusCode, grossAmount, signature)) {
            throw new InvalidWebhookSignatureException("Signature webhook Midtrans tidak valid");
        }

        String transactionStatus = stringValue(payload.get("transaction_status"));
        String transactionId = firstNonBlank(stringValue(payload.get("transaction_id")), orderId);
        String eventKey = "MIDTRANS:" + transactionId + ":" + transactionStatus;
        return process(PaymentGateway.MIDTRANS, eventKey, orderId, transactionId,
                transactionStatus, payload);
    }

    @Transactional
    public WebhookResult handleXendit(String callbackToken, Map<String, Object> payload) {
        if (!xenditClient.verifyCallbackToken(callbackToken)) {
            throw new InvalidWebhookSignatureException("Callback token Xendit tidak valid");
        }

        String externalId = stringValue(payload.get("external_id"));
        String status = stringValue(payload.get("status"));
        String transactionId = firstNonBlank(stringValue(payload.get("id")), externalId);
        String eventKey = "XENDIT:" + transactionId + ":" + status;
        return process(PaymentGateway.XENDIT, eventKey, externalId, transactionId, status, payload);
    }

    private WebhookResult process(PaymentGateway gateway, String eventKey, String orderId,
                                  String transactionId, String gatewayStatus, Map<String, Object> payload) {
        WebhookEvent event = webhookEventRepository.findByEventKey(eventKey).orElse(null);
        if (event != null && event.isProcessed()) {
            return new WebhookResult(true);
        }
        if (event == null) {
            event = WebhookEvent.create(eventKey, gateway, transactionId,
                    gatewayStatus, payload, true);
            event = webhookEventRepository.save(event);
        }

        UUID orderUuid = parseUuid(orderId);
        Payment payment = null;
        if (orderUuid != null) {
            payment = paymentRepository.findByIdForUpdate(orderUuid).orElse(null);
        }
        if (payment == null) {
            payment = paymentRepository.findByExternalTransactionId(transactionId).orElse(null);
        }

        if (payment != null) {
            applyPaymentStatus(payment, gatewayStatus, transactionId, payload);
            event.markProcessed();
            webhookEventRepository.save(event);
            return new WebhookResult(true);
        }

        LoanDisbursement disbursement = null;
        if (orderUuid != null) {
            disbursement = loanDisbursementRepository.findById(orderUuid).orElse(null);
        }
        if (disbursement == null) {
            disbursement = loanDisbursementRepository.findByLoanApplicationId(orderUuid).orElse(null);
        }
        if (disbursement == null && transactionId != null) {
            disbursement = loanDisbursementRepository.findByExternalTransactionId(transactionId).orElse(null);
        }

        if (disbursement != null) {
            applyDisbursementStatus(disbursement, gatewayStatus, transactionId, payload);
            event.markProcessed();
            webhookEventRepository.save(event);
            return new WebhookResult(true);
        }

        event.markFailed("Pembayaran atau pencairan tidak ditemukan untuk webhook");
        webhookEventRepository.save(event);
        return new WebhookResult(false);
    }

    private void applyPaymentStatus(Payment payment, String gatewayStatus, String transactionId,
                                    Map<String, Object> payload) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }
        PaymentStatus newStatus = mapPaymentStatus(gatewayStatus);
        if (newStatus == PaymentStatus.PENDING) {
            payment.setExternalTransactionId(transactionId);
            payment.setWebhookPayload(payload);
            paymentRepository.save(payment);
            return;
        }

        if (newStatus == PaymentStatus.SUCCESS) {
            Instant paidAt = Instant.now();
            payment.markSuccess(transactionId, payload, paidAt);
            try {
                loanServiceClient.markInstallmentPaid(payment.getLoanInstallmentId(), paidAt);
                payment.markSynced();
            } catch (RuntimeException ex) {
                payment.markSyncFailed(ex.getMessage());
                log.warn("Gagal menandai cicilan lunas di loan-service paymentId={}: {}",
                        payment.getId(), ex.getMessage());
            }
            paymentRepository.save(payment);
            publishPaymentReceived(payment);
        } else {
            payment.markFailed(transactionId, payload);
            paymentRepository.save(payment);
            publishPaymentFailed(payment);
        }
    }

    private void applyDisbursementStatus(LoanDisbursement disbursement, String gatewayStatus,
                                         String transactionId, Map<String, Object> payload) {
        if (disbursement.getStatus() == DisbursementStatus.COMPLETED) {
            return;
        }
        disbursement.setExternalTransactionId(transactionId);
        if (isSuccess(gatewayStatus)) {
            disbursement.setStatus(DisbursementStatus.COMPLETED);
            disbursement.setDisbursedAt(Instant.now());
            try {
                loanServiceClient.markDisbursed(
                        disbursement.getLoanApplicationId(), disbursement.getDisbursedAt());
                disbursement.markSynced();
            } catch (RuntimeException ex) {
                disbursement.markSyncFailed(ex.getMessage());
                log.warn("Gagal menandai pinjaman dicairkan di loan-service disbursementId={}: {}",
                        disbursement.getId(), ex.getMessage());
            }
            loanDisbursementRepository.save(disbursement);
            Map<String, Object> eventPayload = new LinkedHashMap<>();
            eventPayload.put("disbursementId", disbursement.getId().toString());
            eventPayload.put("loanApplicationId", disbursement.getLoanApplicationId().toString());
            eventPayload.put("amount", disbursement.getAmount());
            eventPayload.put("paymentGateway", disbursement.getPaymentGateway().name());
            eventPayload.put("externalTransactionId", transactionId);
            eventPayload.put("status", "COMPLETED");
            eventPayload.put("disbursedAt", disbursement.getDisbursedAt().toString());
            paymentEventPublisher.publishAfterCommit(PaymentEventType.DISBURSEMENT_COMPLETED,
                    disbursement.getId().toString(), eventPayload);
        } else if (isFailure(gatewayStatus)) {
            disbursement.setStatus(DisbursementStatus.FAILED);
            loanDisbursementRepository.save(disbursement);
        } else {
            loanDisbursementRepository.save(disbursement);
        }
    }

    private void publishPaymentReceived(Payment payment) {
        InstallmentInfo installment = loadInstallment(payment);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("installmentId", payment.getLoanInstallmentId().toString());
        payload.put("loanApplicationId", installment == null ? null : installment.loanApplicationId().toString());
        payload.put("borrowerId", payment.getBorrowerId().toString());
        payload.put("installmentNumber", installment == null ? null : installment.installmentNumber());
        payload.put("amount", payment.getAmount());
        payload.put("paymentGateway", payment.getPaymentGateway().name());
        payload.put("externalTransactionId", payment.getExternalTransactionId());
        payload.put("paidAt", payment.getPaidAt().toString());
        paymentEventPublisher.publishAfterCommit(PaymentEventType.INSTALLMENT_RECEIVED,
                payment.getId().toString(), payload);
    }

    private void publishPaymentFailed(Payment payment) {
        InstallmentInfo installment = loadInstallment(payment);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("installmentId", payment.getLoanInstallmentId().toString());
        payload.put("loanApplicationId", installment == null ? null : installment.loanApplicationId().toString());
        payload.put("borrowerId", payment.getBorrowerId().toString());
        payload.put("amount", payment.getAmount());
        payload.put("failureReason", "Gateway menolak pembayaran");
        payload.put("retryCount", 0);
        payload.put("failedAt", Instant.now().toString());
        paymentEventPublisher.publishAfterCommit(PaymentEventType.INSTALLMENT_FAILED,
                payment.getId().toString(), payload);
    }

    private InstallmentInfo loadInstallment(Payment payment) {
        try {
            return loanServiceClient.getInstallment(payment.getLoanInstallmentId());
        } catch (RuntimeException ex) {
            log.warn("Gagal memuat detail cicilan paymentId={}: {}", payment.getId(), ex.getMessage());
            return null;
        }
    }

    private PaymentStatus mapPaymentStatus(String gatewayStatus) {
        if (gatewayStatus == null) {
            return PaymentStatus.PENDING;
        }
        String status = gatewayStatus.toUpperCase();
        return switch (status) {
            case "SETTLEMENT", "CAPTURE", "PAID", "SUCCESS", "SETTLED" -> PaymentStatus.SUCCESS;
            case "DENY", "CANCEL", "EXPIRE", "EXPIRED", "FAILURE", "FAILED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private boolean isSuccess(String gatewayStatus) {
        return mapPaymentStatus(gatewayStatus) == PaymentStatus.SUCCESS;
    }

    private boolean isFailure(String gatewayStatus) {
        return mapPaymentStatus(gatewayStatus) == PaymentStatus.FAILED;
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
