package com.koperasi.payment.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanApplicationInfo(
        UUID loanApplicationId,
        UUID borrowerId,
        BigDecimal amount,
        String status
) {
}
