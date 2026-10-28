package com.koperasi.auth.domain.dto;

import com.koperasi.auth.domain.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        @Size(max = 255, message = "Email maksimal 255 karakter")
        String email,

        @NotBlank(message = "Kata sandi wajib diisi")
        @Size(min = 12, max = 72, message = "Kata sandi harus 12-72 karakter")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};:'\",.<>/\\\\|`~]).+$",
                message = "Kata sandi harus mengandung huruf besar, huruf kecil, angka, dan karakter khusus"
        )
        String password,

        @NotBlank(message = "Nomor telepon wajib diisi")
        @Pattern(regexp = "^\\+?62[0-9]{9,13}$", message = "Nomor telepon harus format Indonesia (+62)")
        String phone,

        RoleName role
) {
}
