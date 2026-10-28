package com.koperasi.auth.service;

import com.koperasi.auth.config.PasswordResetProperties;
import com.koperasi.auth.domain.entity.PasswordResetToken;
import com.koperasi.auth.domain.entity.User;
import com.koperasi.auth.event.AuditEventPublisher;
import com.koperasi.auth.event.AuditPayload;
import com.koperasi.auth.exception.InvalidResetTokenException;
import com.koperasi.auth.repository.PasswordResetTokenRepository;
import com.koperasi.auth.repository.RefreshTokenRepository;
import com.koperasi.auth.repository.UserRepository;
import com.koperasi.auth.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ENTITY_TYPE = "users";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailSender emailSender;
    private final PasswordResetProperties properties;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Always behaves as if the email exists so the endpoint cannot be used to enumerate accounts.
     */
    @Transactional
    public void requestReset(String email, String ipAddress, String userAgent) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalized);
        if (userOptional.isEmpty()) {
            log.info("Permintaan reset kata sandi untuk email yang tidak terdaftar");
            return;
        }

        User user = userOptional.get();
        Instant now = Instant.now();
        passwordResetTokenRepository.invalidateAllForUser(user.getId(), now);

        String rawToken = generateToken();
        PasswordResetToken token = PasswordResetToken.create(
                user,
                TokenHasher.hash(rawToken),
                now.plusSeconds(properties.getTtlMinutes() * 60),
                ipAddress,
                userAgent
        );
        passwordResetTokenRepository.save(token);

        emailSender.send(user, rawToken);
        auditEventPublisher.publishAfterCommit(audit(
                user.getId(), "PASSWORD_RESET_REQUESTED", user.getId().toString(),
                Map.of("email", user.getEmail()), ipAddress, userAgent
        ));
        log.info("Token reset kata sandi dibuat userId={}", user.getId());
    }

    @Transactional
    @CacheEvict(cacheNames = "userById", allEntries = true)
    public void reset(String rawToken, String newPassword, String ipAddress, String userAgent) {
        PasswordResetToken token = passwordResetTokenRepository
                .findActiveByTokenHashForUpdate(TokenHasher.hash(rawToken))
                .orElseThrow(() -> new InvalidResetTokenException(
                        "Tautan reset tidak valid atau sudah digunakan"));

        Instant now = Instant.now();
        if (!token.isUsable(now)) {
            throw new InvalidResetTokenException("Tautan reset sudah kedaluwarsa, silakan minta tautan baru");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markUsed(now);
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.invalidateAllForUser(user.getId(), now);

        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
        refreshTokenStore.deleteAllForUser(user.getId());

        auditEventPublisher.publishAfterCommit(audit(
                user.getId(), "PASSWORD_RESET_COMPLETED", user.getId().toString(),
                Map.of("email", user.getEmail()), ipAddress, userAgent
        ));
        log.info("Kata sandi berhasil direset userId={}", user.getId());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AuditPayload audit(UUID actorId, String action, String entityId,
                               Map<String, Object> newValue, String ipAddress, String userAgent) {
        return new AuditPayload(
                actorId.toString(),
                "BORROWER",
                action,
                ENTITY_TYPE,
                entityId,
                null,
                newValue,
                ipAddress,
                userAgent,
                "SUCCESS",
                null
        );
    }
}
