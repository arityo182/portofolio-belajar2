package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.entity.LoanInstallment;
import com.koperasi.loan.domain.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanInstallmentResponse(
        UUID installmentId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal principal,
        BigDecimal interest,
        InstallmentStatus status,
        Instant paidAt
) {
    public static LoanInstallmentResponse from(LoanInstallment installment) {
        return new LoanInstallmentResponse(
                installment.getId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getAmount(),
                installment.getPrincipal(),
                installment.getInterest(),
                installment.getStatus(),
                installment.getPaidAt()
        );
    }
}
