package com.koperasi.payment.domain.entity;

import com.koperasi.payment.domain.enums.PaymentGateway;
import com.koperasi.payment.domain.enums.PaymentStatus;
import com.koperasi.payment.domain.enums.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "loan_installment_id", nullable = false)
    private UUID loanInstallmentId;

    @Column(name = "borrower_id", nullable = false)
    private UUID borrowerId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", nullable = false, length = 20)
    private PaymentGateway paymentGateway;

    @Column(name = "external_transaction_id", length = 255)
    private String externalTransactionId;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "webhook_payload", columnDefinition = "jsonb")
    private Map<String, Object> webhookPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private SyncStatus syncStatus;

    @Column(name = "sync_attempts", nullable = false)
    private int syncAttempts;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "sync_error", columnDefinition = "TEXT")
    private String syncError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Payment create(UUID installmentId, UUID borrowerId, BigDecimal amount,
                                 PaymentGateway gateway, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setLoanInstallmentId(installmentId);
        payment.setBorrowerId(borrowerId);
        payment.setAmount(amount);
        payment.setPaymentGateway(gateway);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setSyncStatus(SyncStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        return payment;
    }

    public void markSuccess(String externalId, Map<String, Object> payload, Instant paidAt) {
        this.status = PaymentStatus.SUCCESS;
        this.externalTransactionId = externalId;
        this.webhookPayload = payload;
        this.paidAt = paidAt;
    }

    public void markFailed(String externalId, Map<String, Object> payload) {
        this.status = PaymentStatus.FAILED;
        this.externalTransactionId = externalId;
        this.webhookPayload = payload;
    }

    public void markSynced() {
        this.syncStatus = SyncStatus.SYNCED;
        this.lastSyncAt = Instant.now();
        this.syncError = null;
    }

    public void markSyncFailed(String error) {
        this.syncStatus = SyncStatus.FAILED;
        this.syncAttempts += 1;
        this.lastSyncAt = Instant.now();
        this.syncError = error != null && error.length() > 1000
                ? error.substring(0, 1000)
                : error;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (syncStatus == null) {
            syncStatus = SyncStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
