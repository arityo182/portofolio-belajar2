package com.koperasi.auth.service;

import com.koperasi.auth.config.JwtProperties;
import com.koperasi.auth.domain.dto.LoginRequest;
import com.koperasi.auth.domain.dto.LoginResponse;
import com.koperasi.auth.domain.dto.RefreshRequest;
import com.koperasi.auth.domain.dto.RegisterRequest;
import com.koperasi.auth.domain.dto.UserResponse;
import com.koperasi.auth.domain.entity.RefreshToken;
import com.koperasi.auth.domain.entity.Role;
import com.koperasi.auth.domain.entity.User;
import com.koperasi.auth.domain.enums.RoleName;
import com.koperasi.auth.domain.enums.UserStatus;
import com.koperasi.auth.event.AuditEventPublisher;
import com.koperasi.auth.event.AuditPayload;
import com.koperasi.auth.exception.AccountSuspendedException;
import com.koperasi.auth.exception.InvalidCredentialsException;
import com.koperasi.auth.exception.ResourceNotFoundException;
import com.koperasi.auth.exception.RoleNotAllowedException;
import com.koperasi.auth.exception.TokenExpiredException;
import com.koperasi.auth.exception.TokenRevokedException;
import com.koperasi.auth.exception.UserAlreadyExistsException;
import com.koperasi.auth.repository.RefreshTokenRepository;
import com.koperasi.auth.repository.RoleRepository;
import com.koperasi.auth.repository.UserRepository;
import com.koperasi.auth.security.JwtPrincipal;
import com.koperasi.auth.util.JwtUtil;
import com.koperasi.auth.util.TokenHasher;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@example.id";
    private static final String PASSWORD = "Koperasi#2026Aman";
    private static final String PASSWORD_HASH = "$2a$04$abcdefghijklmnopqrstuuJ5YQ0d7Q1jY2lYQ2c4u7Vw1x2y3z4";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    private JwtProperties jwtProperties;
    private JwtUtil jwtUtil;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("unit-test-jwt-secret-key-that-is-long-enough-32bytes");
        jwtProperties.setAccessTokenTtlSeconds(900);
        jwtProperties.setRefreshTokenTtlSeconds(604800);
        jwtProperties.setIssuer("koperasi-auth");
        jwtUtil = new JwtUtil(jwtProperties);
        authService = new AuthServiceImpl(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtUtil,
                refreshTokenStore,
                tokenBlacklistService,
                auditEventPublisher
        );
    }

    @Test
    void register_withNewEmail_persistsUserRoleAndAudit() {
        // given
        RegisterRequest request = new RegisterRequest("  New.User@Example.ID ", PASSWORD, "+6281234567890", RoleName.BORROWER);
        Role role = role(RoleName.BORROWER);
        when(userRepository.existsByEmailIgnoreCase("new.user@example.id")).thenReturn(false);
        when(roleRepository.findByName(RoleName.BORROWER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserResponse response = authService.register(request, "device", "127.0.0.1", "JUnit");

        // then
        assertThat(response.userId()).isNotNull();
        assertThat(response.email()).isEqualTo("new.user@example.id");
        assertThat(response.phone()).isEqualTo("+6281234567890");
        assertThat(response.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(response.roles()).containsExactly("BORROWER");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new.user@example.id");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getUserRoles()).hasSize(1);
        assertThat(saved.roleNames()).contains("BORROWER");

        ArgumentCaptor<AuditPayload> auditCaptor = ArgumentCaptor.forClass(AuditPayload.class);
        verify(auditEventPublisher).publishAfterCommit(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("USER_REGISTERED");
        assertThat(auditCaptor.getValue().result()).isEqualTo("SUCCESS");
        verify(auditEventPublisher, never()).publish(any());
    }

    @Test
    void register_withExistingEmail_throwsUserAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "+6281234567890", RoleName.BORROWER);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, "device", "ip", "ua"))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email sudah terdaftar");

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(roleRepository, auditEventPublisher);
    }

    @Test
    void register_withPrivilegedRole_throwsRoleNotAllowedException() {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "+6281234567890", RoleName.ADMIN);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request, "device", "ip", "ua"))
                .isInstanceOf(RoleNotAllowedException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void register_whenRoleSeedMissing_throwsResourceNotFoundException() {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "+6281234567890", RoleName.BORROWER);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(roleRepository.findByName(RoleName.BORROWER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request, "device", "ip", "ua"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_withValidCredentials_returnsAccessAndRefreshTokens() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD), "device", "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);

        Claims accessClaims = jwtUtil.parseAccessToken(response.accessToken());
        assertThat(accessClaims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(accessClaims.getId()).isNotBlank();
        assertThat(jwtUtil.extractRoles(accessClaims)).containsExactly("BORROWER");

        Claims refreshClaims = jwtUtil.parseRefreshToken(response.refreshToken());
        UUID tokenId = UUID.fromString(refreshClaims.getId());
        String tokenHash = TokenHasher.hash(response.refreshToken());
        verify(refreshTokenStore).save(eq(user.getId()), eq(tokenId), eq(tokenHash), eq(Duration.ofSeconds(604800)));
        verify(auditEventPublisher).publishAfterCommit(any(AuditPayload.class));
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentialsExceptionAndAuditsFailure() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD), "device", "ip", "ua"))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<AuditPayload> auditCaptor = ArgumentCaptor.forClass(AuditPayload.class);
        verify(auditEventPublisher).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("LOGIN_FAILED");
        assertThat(auditCaptor.getValue().result()).isEqualTo("FAILURE");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void login_withUnknownEmail_throwsInvalidCredentialsException() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD), "device", "ip", "ua"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(eq(PASSWORD), anyString());
        verify(auditEventPublisher).publish(any(AuditPayload.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_withSuspendedUser_throwsAccountSuspendedException() {
        User user = user(UserStatus.SUSPENDED, RoleName.BORROWER);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD), "device", "ip", "ua"))
                .isInstanceOf(AccountSuspendedException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refresh_withValidToken_rotatesRefreshTokenAndRevokesOldToken() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        UUID oldTokenId = UUID.randomUUID();
        String oldRefreshToken = jwtUtil.generateRefreshToken(user.getId(), oldTokenId);
        String oldTokenHash = TokenHasher.hash(oldRefreshToken);
        RefreshToken stored = RefreshToken.create(
                user, oldTokenId, oldTokenHash, "old-device", Instant.now().plusSeconds(604800)
        );

        when(refreshTokenRepository.findByIdAndTokenHashForUpdate(oldTokenId, oldTokenHash))
                .thenReturn(Optional.of(stored));
        when(refreshTokenStore.find(user.getId(), oldTokenId)).thenReturn(Optional.of(oldTokenHash));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.refresh(
                new RefreshRequest(oldRefreshToken), "new-device", "127.0.0.1", "JUnit"
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank().isNotEqualTo(oldRefreshToken);
        assertThat(stored.isRevoked()).isTrue();
        assertThat(stored.getRevokedAt()).isNotNull();

        Claims newRefreshClaims = jwtUtil.parseRefreshToken(response.refreshToken());
        assertThat(newRefreshClaims.getId()).isNotEqualTo(oldTokenId.toString());
        verify(refreshTokenStore).delete(user.getId(), oldTokenId);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        verify(auditEventPublisher).publishAfterCommit(any(AuditPayload.class));
    }

    @Test
    void refresh_withExpiredStoredToken_throwsTokenExpiredException() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        UUID tokenId = UUID.randomUUID();
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tokenId);
        String hash = TokenHasher.hash(refreshToken);
        RefreshToken stored = RefreshToken.create(
                user, tokenId, hash, "device", Instant.now().minusSeconds(10)
        );
        when(refreshTokenRepository.findByIdAndTokenHashForUpdate(tokenId, hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(refreshToken), "device", "ip", "ua"))
                .isInstanceOf(TokenExpiredException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refresh_withRevokedToken_detectsReuseAndRevokesAllSessions() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        UUID tokenId = UUID.randomUUID();
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tokenId);
        String hash = TokenHasher.hash(refreshToken);
        RefreshToken stored = RefreshToken.create(
                user, tokenId, hash, "device", Instant.now().plusSeconds(604800)
        );
        stored.revoke(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByIdAndTokenHashForUpdate(tokenId, hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(refreshToken), "device", "ip", "ua"))
                .isInstanceOf(TokenRevokedException.class);

        verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()), any(Instant.class));
        verify(refreshTokenStore).deleteAllForUser(user.getId());
        verify(auditEventPublisher).publish(any(AuditPayload.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refresh_whenRedisEntryMissing_detectsReuse() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        UUID tokenId = UUID.randomUUID();
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tokenId);
        String hash = TokenHasher.hash(refreshToken);
        RefreshToken stored = RefreshToken.create(
                user, tokenId, hash, "device", Instant.now().plusSeconds(604800)
        );
        when(refreshTokenRepository.findByIdAndTokenHashForUpdate(tokenId, hash)).thenReturn(Optional.of(stored));
        when(refreshTokenStore.find(user.getId(), tokenId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(refreshToken), "device", "ip", "ua"))
                .isInstanceOf(TokenRevokedException.class);

        verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()), any(Instant.class));
        verify(refreshTokenStore).deleteAllForUser(user.getId());
    }

    @Test
    void refresh_whenTokenNotFound_detectsReuse() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        UUID tokenId = UUID.randomUUID();
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tokenId);
        String hash = TokenHasher.hash(refreshToken);
        when(refreshTokenRepository.findByIdAndTokenHashForUpdate(tokenId, hash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(refreshToken), "device", "ip", "ua"))
                .isInstanceOf(TokenRevokedException.class);

        verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()), any(Instant.class));
        verify(refreshTokenStore).deleteAllForUser(user.getId());
    }

    @Test
    void logout_blacklistsAccessTokenAndRevokesRefreshTokens() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(300);
        JwtPrincipal principal = new JwtPrincipal(
                userId, EMAIL, Set.of("BORROWER"), "access-jti", expiresAt
        );

        authService.logout(principal, "127.0.0.1", "JUnit");

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(tokenBlacklistService).blacklist(eq("access-jti"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(301));
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(Instant.class));
        verify(refreshTokenStore).deleteAllForUser(userId);
        verify(auditEventPublisher).publishAfterCommit(any(AuditPayload.class));
    }

    @Test
    void getUserById_whenUserExists_returnsProfile() {
        User user = user(UserStatus.ACTIVE, RoleName.BORROWER);
        when(userRepository.findWithRolesById(user.getId())).thenReturn(Optional.of(user));

        UserResponse response = authService.getUserById(user.getId());

        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.roles()).contains("BORROWER");
    }

    @Test
    void getUserById_whenUserMissing_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findWithRolesById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(UserStatus status, RoleName roleName) {
        User user = User.create(EMAIL, PASSWORD_HASH, "+6281234567890", status);
        user.addRole(role(roleName));
        return user;
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setId(roleName.ordinal() + 1);
        role.setName(roleName);
        role.setDescription(roleName.name());
        return role;
    }
}
