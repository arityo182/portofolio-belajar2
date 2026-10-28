package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.entity.LoanInstallment;
import com.koperasi.loan.domain.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanInstallmentDetailsResponse(
        UUID installmentId,
        UUID loanApplicationId,
        UUID borrowerId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal principal,
        BigDecimal interest,
        InstallmentStatus status,
        Instant paidAt
) {
    public static LoanInstallmentDetailsResponse from(LoanInstallment installment) {
        return new LoanInstallmentDetailsResponse(
                installment.getId(),
                installment.getLoanApplication().getId(),
                installment.getLoanApplication().getBorrowerId(),
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
