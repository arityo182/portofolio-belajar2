package com.koperasi.auth.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BorrowerProfileRequest(
        @NotBlank(message = "NIK wajib diisi")
        @Pattern(regexp = "[\\d*]{16}", message = "NIK harus 16 digit")
        String nik,

        @Pattern(regexp = "[\\d*]{15,16}", message = "NPWP harus 15-16 digit")
        String npwp,

        @NotBlank(message = "Nama lengkap wajib diisi")
        @Size(max = 255, message = "Nama lengkap maksimal 255 karakter")
        String fullName,

        @NotNull(message = "Tanggal lahir wajib diisi")
        @Past(message = "Tanggal lahir harus di masa lalu")
        LocalDate dateOfBirth,

        @NotBlank(message = "Jenis kelamin wajib diisi")
        @Pattern(regexp = "[MF]", message = "Jenis kelamin harus M atau F")
        String gender,

        @NotBlank(message = "Alamat wajib diisi")
        @Size(max = 500, message = "Alamat maksimal 500 karakter")
        String address,

        @NotBlank(message = "Kota wajib diisi")
        @Size(max = 100, message = "Kota maksimal 100 karakter")
        String city,

        @NotBlank(message = "Provinsi wajib diisi")
        @Size(max = 100, message = "Provinsi maksimal 100 karakter")
        String province,

        @NotBlank(message = "Nama bank wajib diisi")
        @Size(max = 100, message = "Nama bank maksimal 100 karakter")
        String bankName,

        @NotBlank(message = "Nomor rekening wajib diisi")
        @Size(max = 32, message = "Nomor rekening maksimal 32 karakter")
        @Pattern(regexp = "[\\d*]{4,32}", message = "Nomor rekening harus berupa digit")
        String bankAccount,

        @NotNull(message = "Pendapatan bulanan wajib diisi")
        @DecimalMin(value = "0.0", message = "Pendapatan bulanan tidak boleh negatif")
        @Digits(integer = 13, fraction = 2, message = "Format pendapatan tidak valid")
        BigDecimal incomeMonthly,

        @NotBlank(message = "Jenis usaha wajib diisi")
        @Size(max = 100, message = "Jenis usaha maksimal 100 karakter")
        String businessType,

        @NotBlank(message = "Nama usaha wajib diisi")
        @Size(max = 255, message = "Nama usaha maksimal 255 karakter")
        String businessName
) {
}
