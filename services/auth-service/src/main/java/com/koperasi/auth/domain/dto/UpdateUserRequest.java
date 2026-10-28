package com.koperasi.auth.domain.dto;

import com.koperasi.auth.domain.enums.RoleName;
import com.koperasi.auth.domain.enums.UserStatus;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 20, message = "Nomor telepon maksimal 20 karakter")
        String phone,

        RoleName role,

        UserStatus status,

        @Size(min = 12, max = 72, message = "Kata sandi harus 12-72 karakter")
        String password
) {
}
