package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.entity.LoanApplication;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanApplicationOwnerResponse(
        UUID loanApplicationId,
        UUID borrowerId,
        BigDecimal amount,
        String status
) {
    public static LoanApplicationOwnerResponse from(LoanApplication application) {
        return new LoanApplicationOwnerResponse(
                application.getId(),
                application.getBorrowerId(),
                application.getAmount(),
                application.getStatus().name()
        );
    }
}
