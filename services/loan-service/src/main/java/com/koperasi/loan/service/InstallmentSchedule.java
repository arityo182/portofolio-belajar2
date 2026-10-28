package com.koperasi.loan.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentSchedule(
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal principal,
        BigDecimal interest
) {
}
