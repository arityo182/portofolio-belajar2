package com.koperasi.loan.domain.entity;

import com.koperasi.loan.domain.enums.DisbursementStatus;
import com.koperasi.loan.domain.enums.PaymentGateway;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_disbursements", schema = "loan_schema")
@Getter
@Setter
@NoArgsConstructor
public class LoanDisbursement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static LoanDisbursement create(LoanApplication application, BigDecimal amount,
                                          PaymentGateway paymentGateway, String idempotencyKey) {
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setId(UUID.randomUUID());
        disbursement.setLoanApplication(application);
        disbursement.setAmount(amount);
        disbursement.setPaymentGateway(paymentGateway);
        disbursement.setStatus(DisbursementStatus.PENDING);
        disbursement.setIdempotencyKey(idempotencyKey);
        return disbursement;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = DisbursementStatus.PENDING;
        }
    }
}
