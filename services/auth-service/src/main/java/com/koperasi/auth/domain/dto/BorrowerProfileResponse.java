package com.koperasi.auth.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BorrowerProfileResponse(
        UUID profileId,
        String fullName,
        String nik,
        String npwp,
        String dateOfBirth,
        String gender,
        String address,
        String city,
        String province,
        String bankName,
        String bankAccount,
        BigDecimal incomeMonthly,
        String businessType,
        String businessName,
        boolean isVerified,
        Instant verifiedAt,
        String ktpPhotoUrl,
        String selfiePhotoUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
