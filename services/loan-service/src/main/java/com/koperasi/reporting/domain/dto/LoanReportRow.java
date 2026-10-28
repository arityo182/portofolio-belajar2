package com.koperasi.reporting.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanReportRow(
        UUID loanId,
        String borrowerName,
        BigDecimal amount,
        String status,
        Instant disbursedAt,
        BigDecimal outstandingAmount
) {
}
