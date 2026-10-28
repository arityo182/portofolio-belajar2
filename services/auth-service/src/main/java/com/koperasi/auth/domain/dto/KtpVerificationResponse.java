package com.koperasi.auth.domain.dto;

import java.util.UUID;

public record KtpVerificationResponse(
        UUID verificationId,
        String status,
        String ktpPhotoUrl,
        String selfiePhotoUrl
) {
}
