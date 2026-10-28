package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanApplicationResponse(
        UUID loanId,
        UUID borrowerId,
        Integer loanProductId,
        String loanProductName,
        BigDecimal amount,
        Integer tenorMonths,
        String purpose,
        LoanStatus status,
        UUID reviewerId,
        String rejectionReason,
        String reviewNotes,
        Instant submittedAt,
        Instant reviewedAt,
        Instant disbursedAt,
        Instant completedAt,
        Long version,
        Instant createdAt
) {
    public static LoanApplicationResponse from(LoanApplication application) {
        return new LoanApplicationResponse(
                application.getId(),
                application.getBorrowerId(),
                application.getLoanProduct() == null ? null : application.getLoanProduct().getId(),
                application.getLoanProduct() == null ? null : application.getLoanProduct().getName(),
                application.getAmount(),
                application.getTenorMonths(),
                application.getPurpose(),
                application.getStatus(),
                application.getReviewerId(),
                application.getRejectionReason(),
                application.getReviewNotes(),
                application.getSubmittedAt(),
                application.getReviewedAt(),
                application.getDisbursedAt(),
                application.getCompletedAt(),
                application.getVersion(),
                application.getCreatedAt()
        );
    }
}
