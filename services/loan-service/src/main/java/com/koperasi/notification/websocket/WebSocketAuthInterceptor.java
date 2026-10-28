package com.koperasi.notification.websocket;

import com.koperasi.loan.security.JwtPrincipal;
import com.koperasi.loan.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            JwtPrincipal principal = null;
            if (accessor.getSessionAttributes() != null) {
                principal = (JwtPrincipal) accessor.getSessionAttributes()
                        .get(JwtHandshakeInterceptor.PRINCIPAL_ATTRIBUTE);
            }
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtUtil.parseAccessToken(authorization.substring(7).trim());
                    principal = new JwtPrincipal(
                            jwtUtil.extractUserId(claims),
                            claims.get("email", String.class),
                            jwtUtil.extractRoles(claims),
                            jwtUtil.extractJti(claims),
                            jwtUtil.extractExpiration(claims)
                    );
                } catch (RuntimeException ex) {
                    throw new MessageDeliveryException("Token WebSocket tidak valid");
                }
            }
            if (principal == null) {
                throw new MessageDeliveryException("Autentikasi WebSocket diperlukan");
            }
            List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            JwtPrincipal principal = principal(accessor);
            if (principal == null) {
                throw new AccessDeniedException("Autentikasi WebSocket diperlukan");
            }
            String destination = accessor.getDestination();
            String prefix = "/topic/notifications/";
            if (destination == null || !destination.startsWith(prefix)) {
                throw new AccessDeniedException("Topik notifikasi tidak valid");
            }
            String requestedUserId = destination.substring(prefix.length());
            try {
                UUID subscribedUserId = UUID.fromString(requestedUserId);
                if (!principal.userId().equals(subscribedUserId)) {
                    throw new AccessDeniedException("Tidak boleh berlangganan notifikasi pengguna lain");
                }
            } catch (IllegalArgumentException ex) {
                throw new AccessDeniedException("Topik notifikasi tidak valid");
            }
        }
        return message;
    }

    private JwtPrincipal principal(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication
                && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return principal;
        }
        if (accessor.getSessionAttributes() != null) {
            Object principal = accessor.getSessionAttributes().get(JwtHandshakeInterceptor.PRINCIPAL_ATTRIBUTE);
            if (principal instanceof JwtPrincipal jwtPrincipal) {
                return jwtPrincipal;
            }
        }
        return null;
    }
}
