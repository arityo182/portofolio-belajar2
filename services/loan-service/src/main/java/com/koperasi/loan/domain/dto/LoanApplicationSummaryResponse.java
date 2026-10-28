package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanApplicationSummaryResponse(
        UUID loanId,
        UUID borrowerId,
        Integer loanProductId,
        String loanProductName,
        BigDecimal amount,
        Integer tenorMonths,
        LoanStatus status,
        UUID reviewerId,
        Instant submittedAt,
        Instant reviewedAt,
        Instant disbursedAt
) {
    public static LoanApplicationSummaryResponse from(LoanApplication application) {
        return new LoanApplicationSummaryResponse(
                application.getId(),
                application.getBorrowerId(),
                application.getLoanProduct() == null ? null : application.getLoanProduct().getId(),
                application.getLoanProduct() == null ? null : application.getLoanProduct().getName(),
                application.getAmount(),
                application.getTenorMonths(),
                application.getStatus(),
                application.getReviewerId(),
                application.getSubmittedAt(),
                application.getReviewedAt(),
                application.getDisbursedAt()
        );
    }
}
