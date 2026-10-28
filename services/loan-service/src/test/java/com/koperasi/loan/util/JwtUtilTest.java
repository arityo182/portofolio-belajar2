package com.koperasi.loan.util;

import com.koperasi.loan.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "unit-test-jwt-secret-key-that-is-long-enough-32bytes";
    private JwtProperties properties;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("koperasi-auth");
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void parseAccessToken_extractsClaimsAndRoles() {
        UUID userId = UUID.randomUUID();
        String token = token(userId, List.of("REVIEWER", "ADMIN"), "access", "koperasi-auth",
                Date.from(Instant.now().plusSeconds(900)), SECRET);

        Claims claims = jwtUtil.parseAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getId()).isNotBlank();
        assertThat(jwtUtil.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtUtil.extractJti(claims)).isEqualTo(claims.getId());
        assertThat(jwtUtil.extractRoles(claims)).containsExactlyInAnyOrder("REVIEWER", "ADMIN");
        assertThat(jwtUtil.extractExpiration(claims)).isAfter(Instant.now());
    }

    @Test
    void parseAccessToken_withRefreshType_throwsJwtException() {
        String token = token(UUID.randomUUID(), List.of("BORROWER"), "refresh", "koperasi-auth",
                Date.from(Instant.now().plusSeconds(900)), SECRET);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Tipe token");
    }

    @Test
    void parseAccessToken_withDifferentSignature_throwsJwtException() {
        String token = token(UUID.randomUUID(), List.of("BORROWER"), "access", "koperasi-auth",
                Date.from(Instant.now().plusSeconds(900)),
                "another-test-signing-secret-that-is-long-enough-32bytes");

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseAccessToken_withExpiredToken_throwsExpiredJwtException() {
        String token = token(UUID.randomUUID(), List.of("BORROWER"), "access", "koperasi-auth",
                Date.from(Instant.now().minusSeconds(10)), SECRET);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseAccessToken_withWrongIssuer_throwsJwtException() {
        String token = token(UUID.randomUUID(), List.of("BORROWER"), "access", "other-issuer",
                Date.from(Instant.now().plusSeconds(900)), SECRET);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_withBase64Secret_works() {
        JwtProperties base64Properties = new JwtProperties();
        base64Properties.setSecret("base64:" + Base64.getEncoder().encodeToString(SECRET.getBytes()));
        base64Properties.setIssuer("koperasi-auth");
        JwtUtil base64Jwt = new JwtUtil(base64Properties);
        String token = token(UUID.randomUUID(), List.of("BORROWER"), "access", "koperasi-auth",
                Date.from(Instant.now().plusSeconds(900)), SECRET);

        assertThat(base64Jwt.parseAccessToken(token)).isNotNull();
    }

    @Test
    void constructor_withMissingOrShortSecret_throwsIllegalStateException() {
        JwtProperties missing = new JwtProperties();
        missing.setIssuer("koperasi-auth");
        assertThatThrownBy(() -> new JwtUtil(missing)).isInstanceOf(IllegalStateException.class);

        JwtProperties shortSecret = new JwtProperties();
        shortSecret.setSecret("short");
        shortSecret.setIssuer("koperasi-auth");
        assertThatThrownBy(() -> new JwtUtil(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bit");
    }

    @Test
    void extractRoles_withoutRolesClaim_returnsEmptySet() {
        Claims claims = Jwts.claims().subject(UUID.randomUUID().toString()).build();

        assertThat(jwtUtil.extractRoles(claims)).isEmpty();
    }

    private String token(UUID userId, List<String> roles, String type, String issuer, Date expiration, String secret) {
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(expiration)
                .claim("type", type)
                .claim("roles", roles)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
