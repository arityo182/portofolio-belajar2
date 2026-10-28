package com.koperasi.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token reset wajib diisi")
        @Size(max = 512, message = "Token reset tidak valid")
        String token,

        @NotBlank(message = "Kata sandi baru wajib diisi")
        @Size(min = 12, max = 72, message = "Kata sandi harus 12-72 karakter")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};:'\",.<>/\\\\|`~]).+$",
                message = "Kata sandi harus mengandung huruf besar, huruf kecil, angka, dan karakter khusus"
        )
        String newPassword
) {
}
