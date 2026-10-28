package com.koperasi.auth.util;

import com.koperasi.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtUtil {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;
    private final String issuer;

    public JwtUtil(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(properties.getSecret()));
        this.accessTokenTtlSeconds = properties.getAccessTokenTtlSeconds();
        this.refreshTokenTtlSeconds = properties.getRefreshTokenTtlSeconds();
        this.issuer = properties.getIssuer();
        if (accessTokenTtlSeconds <= 0 || refreshTokenTtlSeconds <= 0) {
            throw new IllegalStateException("JWT TTL harus lebih besar dari nol");
        }
    }

    public String generateAccessToken(UUID userId, String email, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, UUID tokenId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(tokenId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenTtlSeconds)))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parseToken(token);
        requireType(claims, TYPE_ACCESS);
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseToken(token);
        requireType(claims, TYPE_REFRESH);
        return claims;
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::toUpperCase).collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    public Instant extractExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    public Duration remainingValidity(Claims claims) {
        Duration remaining = Duration.between(Instant.now(), extractExpiration(claims));
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    private void requireType(Claims claims, String expectedType) {
        String actualType = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(actualType)) {
            throw new JwtException("Tipe token tidak valid");
        }
    }

    private static byte[] resolveSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET wajib dikonfigurasi");
        }
        byte[] keyBytes;
        if (secret.startsWith("base64:")) {
            keyBytes = Base64.getDecoder().decode(secret.substring("base64:".length()));
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET minimal 256 bit (32 byte)");
        }
        return keyBytes;
    }
}
