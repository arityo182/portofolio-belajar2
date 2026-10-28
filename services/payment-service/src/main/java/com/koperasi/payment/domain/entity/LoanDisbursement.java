package com.koperasi.payment.domain.entity;

import com.koperasi.payment.domain.enums.DisbursementStatus;
import com.koperasi.payment.domain.enums.PaymentGateway;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_disbursements", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class LoanDisbursement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "loan_application_id", nullable = false, unique = true)
    private UUID loanApplicationId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", nullable = false, length = 20)
    private PaymentGateway paymentGateway;

    @Column(name = "external_transaction_id", length = 255)
    private String externalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DisbursementStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

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

    public static LoanDisbursement create(UUID loanApplicationId, BigDecimal amount,
                                          PaymentGateway gateway, String idempotencyKey) {
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setId(UUID.randomUUID());
        disbursement.setLoanApplicationId(loanApplicationId);
        disbursement.setAmount(amount);
        disbursement.setPaymentGateway(gateway);
        disbursement.setStatus(DisbursementStatus.PENDING);
        disbursement.setSyncStatus(SyncStatus.PENDING);
        disbursement.setIdempotencyKey(idempotencyKey);
        return disbursement;
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
            status = DisbursementStatus.PENDING;
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
