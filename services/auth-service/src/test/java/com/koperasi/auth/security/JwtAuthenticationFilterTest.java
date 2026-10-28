package com.koperasi.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.auth.config.JwtProperties;
import com.koperasi.auth.service.TokenBlacklistService;
import com.koperasi.auth.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private TokenBlacklistService tokenBlacklistService;
    private RestAuthenticationEntryPoint authenticationEntryPoint;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("filter-unit-test-secret-key-that-is-long-enough-32bytes");
        properties.setAccessTokenTtlSeconds(900);
        properties.setRefreshTokenTtlSeconds(604800);
        properties.setIssuer("koperasi-auth");
        jwtUtil = new JwtUtil(properties);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        authenticationEntryPoint = new RestAuthenticationEntryPoint(new ObjectMapper());
        filter = new JwtAuthenticationFilter(jwtUtil, tokenBlacklistService, authenticationEntryPoint);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilter_withoutAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> observed = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> observed.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(observed.get()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_withValidToken_setsAuthenticationAndPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, "user@example.id", Set.of("BORROWER"));
        when(tokenBlacklistService.isBlacklisted(jwtUtil.parseAccessToken(token).getId())).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> observed = new AtomicReference<>();
        AtomicReference<String> mdcUserId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            observed.set(SecurityContextHolder.getContext().getAuthentication());
            mdcUserId.set(MDC.get("userId"));
        });

        assertThat(observed.get()).isNotNull();
        assertThat(observed.get().getPrincipal()).isInstanceOf(JwtPrincipal.class);
        JwtPrincipal principal = (JwtPrincipal) observed.get().getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("user@example.id");
        assertThat(principal.roles()).containsExactly("BORROWER");
        assertThat(observed.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_BORROWER");
        assertThat(mdcUserId.get()).isEqualTo(userId.toString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void doFilter_withBlacklistedToken_returnsTokenRevoked() throws Exception {
        String token = jwtUtil.generateAccessToken(UUID.randomUUID(), "user@example.id", Set.of("BORROWER"));
        when(tokenBlacklistService.isBlacklisted(jwtUtil.parseAccessToken(token).getId())).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be invoked");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TOKEN_REVOKED");
        assertThat(response.getContentType()).startsWith("application/json");
    }

    @Test
    void doFilter_withMalformedToken_returnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer not-a-valid-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be invoked");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }

    @Test
    void doFilter_withExpiredToken_returnsTokenExpired() throws Exception {
        JwtUtil expiredJwtUtil = mock(JwtUtil.class);
        when(expiredJwtUtil.parseAccessToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));
        JwtAuthenticationFilter expiredFilter =
                new JwtAuthenticationFilter(expiredJwtUtil, tokenBlacklistService, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        expiredFilter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be invoked");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TOKEN_EXPIRED");
    }

    @Test
    void doFilter_withTokenMissingJti_rejectsTokenAsInvalid() throws Exception {
        JwtUtil headerlessJwtUtil = mock(JwtUtil.class);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject(UUID.randomUUID().toString())
                .add("type", "access")
                .add("roles", java.util.List.of("BORROWER"))
                .build();
        when(headerlessJwtUtil.parseAccessToken("token-without-jti")).thenReturn(claims);
        when(headerlessJwtUtil.extractJti(claims)).thenReturn(null);
        JwtAuthenticationFilter headerlessFilter =
                new JwtAuthenticationFilter(headerlessJwtUtil, tokenBlacklistService, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer token-without-jti");
        MockHttpServletResponse response = new MockHttpServletResponse();

        headerlessFilter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be invoked");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }
}
