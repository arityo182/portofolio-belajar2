package com.koperasi.loan.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String email,
        Set<String> roles,
        String jti,
        Instant expiresAt
) {
}
