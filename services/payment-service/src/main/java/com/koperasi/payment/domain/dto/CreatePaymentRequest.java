package com.koperasi.payment.domain.dto;

import com.koperasi.payment.domain.enums.PaymentGateway;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "Gateway pembayaran wajib diisi")
        PaymentGateway paymentGateway,

        @NotNull(message = "Jumlah pembayaran wajib diisi")
        @DecimalMin(value = "0.01", message = "Jumlah pembayaran harus lebih besar dari nol")
        @Digits(integer = 13, fraction = 2, message = "Format jumlah pembayaran tidak valid")
        BigDecimal amount
) {
}
