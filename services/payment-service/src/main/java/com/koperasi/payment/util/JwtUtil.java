package com.koperasi.payment.util;

import com.koperasi.payment.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";

    private final SecretKey signingKey;
    private final String issuer;

    public JwtUtil(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(properties.getSecret()));
        this.issuer = properties.getIssuer();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Tipe token tidak valid");
        }
        return claims;
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    public Set<String> extractRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::toUpperCase)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    public Instant extractExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    private static byte[] resolveSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET wajib dikonfigurasi");
        }
        byte[] keyBytes = secret.startsWith("base64:")
                ? Base64.getDecoder().decode(secret.substring("base64:".length()))
                : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET minimal 256 bit (32 byte)");
        }
        return keyBytes;
    }
}
