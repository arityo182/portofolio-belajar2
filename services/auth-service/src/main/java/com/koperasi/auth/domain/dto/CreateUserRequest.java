package com.koperasi.auth.domain.dto;

import com.koperasi.auth.domain.enums.RoleName;
import com.koperasi.auth.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        @Size(max = 255, message = "Email maksimal 255 karakter")
        String email,

        @NotBlank(message = "Kata sandi wajib diisi")
        @Size(min = 12, max = 72, message = "Kata sandi harus 12-72 karakter")
        String password,

        @Size(max = 20, message = "Nomor telepon maksimal 20 karakter")
        String phone,

        @NotNull(message = "Peran wajib diisi")
        RoleName role,

        UserStatus status
) {
}
