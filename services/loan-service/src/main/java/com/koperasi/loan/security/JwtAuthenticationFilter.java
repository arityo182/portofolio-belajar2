package com.koperasi.loan.security;

import com.koperasi.loan.exception.ErrorCode;
import com.koperasi.loan.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = jwtUtil.parseAccessToken(token);
            String jti = jwtUtil.extractJti(claims);
            if (jti == null || jti.isBlank()) {
                reject(request, response, ErrorCode.UNAUTHORIZED, "Token tidak valid");
                return;
            }

            UUID userId = jwtUtil.extractUserId(claims);
            Set<String> roles = jwtUtil.extractRoles(claims);
            JwtPrincipal principal = new JwtPrincipal(
                    userId,
                    claims.get("email", String.class),
                    roles,
                    jti,
                    jwtUtil.extractExpiration(claims)
            );

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MDC.put("userId", userId.toString());

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            reject(request, response, ErrorCode.UNAUTHORIZED, "Token telah kedaluwarsa");
        } catch (JwtException | IllegalArgumentException ex) {
            reject(request, response, ErrorCode.UNAUTHORIZED, "Token tidak valid");
        } finally {
            MDC.remove("userId");
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message)
            throws IOException, ServletException {
        request.setAttribute(RestAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, code);
        request.setAttribute(RestAuthenticationEntryPoint.ERROR_MESSAGE_ATTRIBUTE, message);
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response,
                new InsufficientAuthenticationException(message));
    }
}
