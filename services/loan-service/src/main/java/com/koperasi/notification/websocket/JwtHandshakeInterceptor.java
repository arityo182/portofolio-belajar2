package com.koperasi.notification.websocket;

import com.koperasi.loan.security.JwtPrincipal;
import com.koperasi.loan.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_ATTRIBUTE = "jwtPrincipal";

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            // The WebSocket handshake is unauthenticated; STOMP CONNECT enforces the
            // bearer token via a native header so JWTs never appear in the URL.
            return true;
        }
        try {
            Claims claims = jwtUtil.parseAccessToken(token.substring("Bearer ".length()).trim());
            JwtPrincipal principal = new JwtPrincipal(
                    jwtUtil.extractUserId(claims),
                    claims.get("email", String.class),
                    jwtUtil.extractRoles(claims),
                    jwtUtil.extractJti(claims),
                    jwtUtil.extractExpiration(claims)
            );
            attributes.put(PRINCIPAL_ATTRIBUTE, principal);
            return true;
        } catch (RuntimeException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
