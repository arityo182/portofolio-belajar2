package com.koperasi.auth.service;

import com.koperasi.auth.config.PasswordResetProperties;
import com.koperasi.auth.domain.entity.PasswordResetToken;
import com.koperasi.auth.domain.entity.User;
import com.koperasi.auth.domain.enums.UserStatus;
import com.koperasi.auth.event.AuditEventPublisher;
import com.koperasi.auth.exception.InvalidResetTokenException;
import com.koperasi.auth.repository.PasswordResetTokenRepository;
import com.koperasi.auth.repository.RefreshTokenRepository;
import com.koperasi.auth.repository.UserRepository;
import com.koperasi.auth.util.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetEmailSender emailSender;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setTtlMinutes(30);
        properties.setResetUrl("http://localhost:3000/reset-password");
        service = new PasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                refreshTokenStore,
                passwordEncoder,
                emailSender,
                properties,
                auditEventPublisher
        );
        user = User.create("budi@example.id", "old-hash", "+6281234567890", UserStatus.ACTIVE);
    }

    @Test
    void requestReset_whenEmailUnknown_doesNotRevealExistenceOrCreateToken() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.id")).thenReturn(Optional.empty());

        service.requestReset("  Unknown@Example.ID ", "127.0.0.1", "JUnit");

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailSender);
    }

    @Test
    void requestReset_whenEmailExists_storesHashedTokenAndSendsEmail() {
        when(userRepository.findByEmailIgnoreCase("budi@example.id")).thenReturn(Optional.of(user));

        service.requestReset("Budi@Example.ID", "127.0.0.1", "JUnit");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        verify(passwordResetTokenRepository).invalidateAllForUser(eq(user.getId()), any(Instant.class));

        ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(user), rawToken.capture());
        assertThat(TokenHasher.hash(rawToken.getValue())).isEqualTo(saved.getTokenHash());
    }

    @Test
    void reset_whenTokenMissing_throwsInvalidResetToken() {
        when(passwordResetTokenRepository.findActiveByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("bad-token", "Koperasi#2026Baru", "127.0.0.1", "JUnit"))
                .isInstanceOf(InvalidResetTokenException.class)
                .hasMessageContaining("tidak valid");
    }

    @Test
    void reset_whenTokenExpired_throwsInvalidResetToken() {
        PasswordResetToken token = PasswordResetToken.create(
                user, TokenHasher.hash("raw-token"), Instant.now().minusSeconds(30), "127.0.0.1", "JUnit");
        when(passwordResetTokenRepository.findActiveByTokenHashForUpdate(TokenHasher.hash("raw-token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.reset("raw-token", "Koperasi#2026Baru", "127.0.0.1", "JUnit"))
                .isInstanceOf(InvalidResetTokenException.class)
                .hasMessageContaining("kedaluwarsa");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void reset_whenTokenValid_updatesPasswordRevokesSessionsAndAudits() {
        PasswordResetToken token = PasswordResetToken.create(
                user, TokenHasher.hash("raw-token"), Instant.now().plusSeconds(600), "127.0.0.1", "JUnit");
        when(passwordResetTokenRepository.findActiveByTokenHashForUpdate(TokenHasher.hash("raw-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("Koperasi#2026Baru")).thenReturn("new-hash");

        service.reset("raw-token", "Koperasi#2026Baru", "127.0.0.1", "JUnit");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(passwordResetTokenRepository).invalidateAllForUser(eq(user.getId()), any(Instant.class));
        verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()), any(Instant.class));
        verify(refreshTokenStore).deleteAllForUser(user.getId());
        verify(auditEventPublisher).publishAfterCommit(any());
    }
}
