package com.koperasi.auth.util;

import com.koperasi.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtProperties properties;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("unit-test-jwt-secret-key-that-is-long-enough-32bytes");
        properties.setAccessTokenTtlSeconds(900);
        properties.setRefreshTokenTtlSeconds(604800);
        properties.setIssuer("koperasi-auth");
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void generateAccessToken_includesSubjectJtiRolesAndExpiry() {
        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateAccessToken(userId, "budi@example.id", Set.of("BORROWER", "INVESTOR"));

        Claims claims = jwtUtil.parseAccessToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuer()).isEqualTo("koperasi-auth");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("email", String.class)).isEqualTo("budi@example.id");
        assertThat(jwtUtil.extractRoles(claims)).containsExactlyInAnyOrder("BORROWER", "INVESTOR");
        assertThat(jwtUtil.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtUtil.extractJti(claims)).isEqualTo(claims.getId());
        assertThat(jwtUtil.extractExpiration(claims)).isAfter(java.time.Instant.now());
        assertThat(jwtUtil.getAccessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    void generateRefreshToken_includesTokenIdAndRefreshType() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        String token = jwtUtil.generateRefreshToken(userId, tokenId);

        Claims claims = jwtUtil.parseRefreshToken(token);
        assertThat(claims.getId()).isEqualTo(tokenId.toString());
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(jwtUtil.getRefreshTokenTtlSeconds()).isEqualTo(604800);
    }

    @Test
    void parseAccessToken_withRefreshToken_throwsJwtException() {
        String refreshToken = jwtUtil.generateRefreshToken(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(refreshToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Tipe token");
    }

    @Test
    void parseRefreshToken_withAccessToken_throwsJwtException() {
        String accessToken = jwtUtil.generateAccessToken(UUID.randomUUID(), "user@example.id", Set.of("BORROWER"));

        assertThatThrownBy(() -> jwtUtil.parseRefreshToken(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseToken_withDifferentSigningKey_throwsJwtException() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("another-unit-test-secret-key-that-is-32bytes-long");
        otherProperties.setIssuer("koperasi-auth");
        JwtUtil otherJwt = new JwtUtil(otherProperties);
        String foreignToken = otherJwt.generateAccessToken(UUID.randomUUID(), "x@example.id", Set.of("BORROWER"));

        assertThatThrownBy(() -> jwtUtil.parseToken(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseRefreshToken_whenExpired_throwsExpiredJwtException() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("unit-test-jwt-secret-key-that-is-long-enough-32bytes");
        shortLived.setAccessTokenTtlSeconds(60);
        shortLived.setRefreshTokenTtlSeconds(1);
        shortLived.setIssuer("koperasi-auth");
        JwtUtil shortLivedJwt = new JwtUtil(shortLived);
        String token = shortLivedJwt.generateRefreshToken(UUID.randomUUID(), UUID.randomUUID());

        Thread.sleep(1200);

        assertThatThrownBy(() -> shortLivedJwt.parseRefreshToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void constructor_withBase64Secret_createsValidKey() {
        JwtProperties base64Properties = new JwtProperties();
        base64Properties.setSecret("base64:" + Base64.getEncoder().encodeToString(
                "0123456789012345678901234567890123456789".getBytes()));
        base64Properties.setIssuer("koperasi-auth");
        JwtUtil base64Jwt = new JwtUtil(base64Properties);

        String token = base64Jwt.generateAccessToken(UUID.randomUUID(), "user@example.id", Set.of("BORROWER"));

        assertThat(base64Jwt.parseAccessToken(token).getSubject()).isNotBlank();
    }

    @Test
    void constructor_withShortSecret_throwsIllegalStateException() {
        JwtProperties shortSecret = new JwtProperties();
        shortSecret.setSecret("too-short");
        shortSecret.setIssuer("koperasi-auth");

        assertThatThrownBy(() -> new JwtUtil(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bit");
    }

    @Test
    void constructor_withoutSecret_throwsIllegalStateException() {
        JwtProperties missingSecret = new JwtProperties();
        missingSecret.setIssuer("koperasi-auth");

        assertThatThrownBy(() -> new JwtUtil(missingSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void remainingValidity_forFreshToken_isPositive() {
        Claims claims = jwtUtil.parseAccessToken(
                jwtUtil.generateAccessToken(UUID.randomUUID(), "user@example.id", Set.of("BORROWER")));

        Duration remaining = jwtUtil.remainingValidity(claims);

        assertThat(remaining).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(900));
    }
}
