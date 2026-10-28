package com.koperasi.loan.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLoanApplicationRequest(
        @NotNull(message = "Produk pinjaman wajib diisi")
        Integer loanProductId,

        @NotNull(message = "Jumlah pinjaman wajib diisi")
        @DecimalMin(value = "0.01", message = "Jumlah pinjaman harus lebih besar dari nol")
        @Digits(integer = 13, fraction = 2, message = "Format jumlah pinjaman tidak valid")
        BigDecimal amount,

        @NotNull(message = "Tenor wajib diisi")
        @Min(value = 1, message = "Tenor minimal 1 bulan")
        @Max(value = 60, message = "Tenor maksimal 60 bulan")
        Integer tenorMonths,

        @NotBlank(message = "Tujuan pinjaman wajib diisi")
        @Size(max = 1000, message = "Tujuan pinjaman maksimal 1000 karakter")
        String purpose
) {
}
