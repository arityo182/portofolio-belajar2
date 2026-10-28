package com.koperasi.loan.domain.entity;

import com.koperasi.loan.domain.enums.InstallmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "loan_installments",
        schema = "loan_schema",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_loan_installments_app_number",
                columnNames = {"loan_application_id", "installment_number"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class LoanInstallment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    @Column(name = "interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal interest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstallmentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static LoanInstallment create(LoanApplication application, int number, LocalDate dueDate,
                                         BigDecimal amount, BigDecimal principal, BigDecimal interest) {
        LoanInstallment installment = new LoanInstallment();
        installment.setId(UUID.randomUUID());
        installment.setLoanApplication(application);
        installment.setInstallmentNumber(number);
        installment.setDueDate(dueDate);
        installment.setAmount(amount);
        installment.setPrincipal(principal);
        installment.setInterest(interest);
        installment.setStatus(InstallmentStatus.PENDING);
        return installment;
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
            status = InstallmentStatus.PENDING;
        }
    }
}
