package com.koperasi.payment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.payment.config.JwtProperties;
import com.koperasi.payment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "filter-unit-test-secret-key-that-is-long-enough-32bytes";

    private JwtUtil jwtUtil;
    private RestAuthenticationEntryPoint entryPoint;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("koperasi-auth");
        jwtUtil = new JwtUtil(properties);
        entryPoint = new RestAuthenticationEntryPoint(new ObjectMapper());
        filter = new JwtAuthenticationFilter(jwtUtil, entryPoint);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilter_withoutAuthorization_continuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> observed = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> observed.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(observed.get()).isNull();
    }

    @Test
    void doFilter_withValidToken_setsPrincipalAndAuthorities() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = token(userId, List.of("BORROWER"), Instant.now().plusSeconds(900));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/x");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> observed = new AtomicReference<>();
        AtomicReference<String> mdcUserId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            observed.set(SecurityContextHolder.getContext().getAuthentication());
            mdcUserId.set(MDC.get("userId"));
        });

        assertThat(observed.get()).isNotNull();
        JwtPrincipal principal = (JwtPrincipal) observed.get().getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(observed.get().getAuthorities()).extracting("authority").containsExactly("ROLE_BORROWER");
        assertThat(mdcUserId.get()).isEqualTo(userId.toString());
    }

    @Test
    void doFilter_withExpiredToken_returnsUnauthorized() throws Exception {
        String token = token(UUID.randomUUID(), List.of("BORROWER"), Instant.now().minusSeconds(10));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/x");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be called");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }

    @Test
    void doFilter_withMalformedToken_returnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/x");
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be called");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doFilter_withTokenMissingJti_rejectsToken() throws Exception {
        JwtUtil mockedJwt = mock(JwtUtil.class);
        Claims claims = Jwts.claims().subject(UUID.randomUUID().toString()).add("type", "access").build();
        when(mockedJwt.parseAccessToken("token")).thenReturn(claims);
        when(mockedJwt.extractJti(claims)).thenReturn(null);
        JwtAuthenticationFilter headerlessFilter = new JwtAuthenticationFilter(mockedJwt, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/x");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        headerlessFilter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be called");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private String token(UUID userId, List<String> roles, Instant expiration) {
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuer("koperasi-auth")
                .issuedAt(new Date())
                .expiration(Date.from(expiration))
                .claim("type", "access")
                .claim("roles", roles)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
