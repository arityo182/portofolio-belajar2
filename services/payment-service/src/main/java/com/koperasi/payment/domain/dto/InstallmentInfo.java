package com.koperasi.payment.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentInfo(
        UUID installmentId,
        UUID loanApplicationId,
        UUID borrowerId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        String installmentStatus,
        Instant paidAt
) {
    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(installmentStatus);
    }
}
