package com.koperasi.loan.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LoanProductRequest(
        @NotBlank(message = "Nama produk wajib diisi")
        @Size(max = 100, message = "Nama produk maksimal 100 karakter")
        String name,

        @NotNull(message = "Nominal minimum wajib diisi")
        @DecimalMin(value = "1.00", message = "Nominal minimum harus lebih dari nol")
        @Digits(integer = 13, fraction = 2, message = "Format nominal minimum tidak valid")
        BigDecimal minAmount,

        @NotNull(message = "Nominal maksimum wajib diisi")
        @DecimalMin(value = "1.00", message = "Nominal maksimum harus lebih dari nol")
        @Digits(integer = 13, fraction = 2, message = "Format nominal maksimum tidak valid")
        BigDecimal maxAmount,

        @NotNull(message = "Suku bunga wajib diisi")
        @DecimalMin(value = "0.00", message = "Suku bunga tidak boleh negatif")
        @Digits(integer = 3, fraction = 2, message = "Format suku bunga tidak valid")
        BigDecimal interestRateAnnual,

        @NotNull(message = "Tenor minimum wajib diisi")
        @Min(value = 1, message = "Tenor minimum minimal 1 bulan")
        @Max(value = 60, message = "Tenor minimum maksimal 60 bulan")
        Integer minTenorMonths,

        @NotNull(message = "Tenor maksimum wajib diisi")
        @Min(value = 1, message = "Tenor maksimum minimal 1 bulan")
        @Max(value = 60, message = "Tenor maksimum maksimal 60 bulan")
        Integer maxTenorMonths,

        Boolean active
) {
}
