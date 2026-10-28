package com.koperasi.payment.domain.dto;

import com.koperasi.payment.domain.enums.PaymentGateway;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDisbursementRequest(
        @NotNull(message = "loanApplicationId wajib diisi")
        UUID loanApplicationId,

        @NotNull(message = "Jumlah pencairan wajib diisi")
        @DecimalMin(value = "0.01", message = "Jumlah pencairan harus lebih besar dari nol")
        @Digits(integer = 13, fraction = 2, message = "Format jumlah pencairan tidak valid")
        BigDecimal amount,

        @NotNull(message = "Gateway pembayaran wajib diisi")
        PaymentGateway paymentGateway
) {
}
