package com.koperasi.auth.service;

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
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final Set<RoleName> SELF_REGISTERABLE_ROLES = EnumSet.of(RoleName.BORROWER, RoleName.INVESTOR);
    private static final String TOKEN_TYPE = "Bearer";
    private static final String ENTITY_TYPE = "users";
    private static final String ANONYMOUS_ROLE = "ANONYMOUS";
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditEventPublisher auditEventPublisher;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request, String deviceInfo, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException();
        }

        RoleName roleName = request.role() == null ? RoleName.BORROWER : request.role();
        if (!SELF_REGISTERABLE_ROLES.contains(roleName)) {
            throw new RoleNotAllowedException("Registrasi mandiri hanya diperbolehkan untuk role BORROWER atau INVESTOR");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role " + roleName + " tidak ditemukan"));

        User user = User.create(
                email,
                passwordEncoder.encode(request.password()),
                request.phone(),
                UserStatus.PENDING_VERIFICATION
        );
        user.addRole(role);

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException();
        }

        auditEventPublisher.publishAfterCommit(audit(
                saved.getId(), roleName.name(), "USER_REGISTERED", saved.getId().toString(),
                null, Map.of("email", saved.getEmail(), "status", saved.getStatus().name(), "role", roleName.name()),
                ipAddress, userAgent, "SUCCESS"
        ));

        log.info("User berhasil terdaftar id={} role={}", saved.getId(), roleName);
        return UserResponse.from(saved);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String deviceInfo, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);

        if (userOptional.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            auditEventPublisher.publish(audit(
                    null, ANONYMOUS_ROLE, "LOGIN_FAILED", ENTITY_TYPE, null,
                    null, Map.of("reason", "USER_NOT_FOUND"), ipAddress, userAgent, "FAILURE"
            ));
            throw new InvalidCredentialsException();
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditEventPublisher.publish(audit(
                    user.getId(), primaryRole(user.roleNames()), "LOGIN_FAILED", ENTITY_TYPE, user.getId().toString(),
                    null, Map.of("reason", "INVALID_PASSWORD"), ipAddress, userAgent, "FAILURE"
            ));
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            auditEventPublisher.publish(audit(
                    user.getId(), primaryRole(user.roleNames()), "LOGIN_FAILED", ENTITY_TYPE, user.getId().toString(),
                    null, Map.of("reason", "ACCOUNT_SUSPENDED"), ipAddress, userAgent, "FAILURE"
            ));
            throw new AccountSuspendedException();
        }

        LoginResponse response = issueTokens(user, deviceInfo);
        auditEventPublisher.publishAfterCommit(audit(
                user.getId(), primaryRole(user.roleNames()), "LOGIN_SUCCESS", ENTITY_TYPE, user.getId().toString(),
                null, Map.of("status", user.getStatus().name()), ipAddress, userAgent, "SUCCESS"
        ));
        log.info("Login berhasil userId={}", user.getId());
        return response;
    }

    @Override
    @Transactional(noRollbackFor = {TokenRevokedException.class, AccountSuspendedException.class})
    public LoginResponse refresh(RefreshRequest request, String deviceInfo, String ipAddress, String userAgent) {
        Claims claims;
        try {
            claims = jwtUtil.parseRefreshToken(request.refreshToken());
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Refresh token telah kedaluwarsa");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException();
        }

        UUID userId;
        UUID tokenId;
        try {
            userId = jwtUtil.extractUserId(claims);
            tokenId = UUID.fromString(jwtUtil.extractJti(claims));
        } catch (RuntimeException ex) {
            throw new InvalidCredentialsException();
        }

        String tokenHash = TokenHasher.hash(request.refreshToken());
        Optional<RefreshToken> storedOptional =
                refreshTokenRepository.findByIdAndTokenHashForUpdate(tokenId, tokenHash);
        if (storedOptional.isEmpty()) {
            throw reuseDetected(userId, ipAddress, userAgent);
        }

        RefreshToken stored = storedOptional.get();
        Instant now = Instant.now();
        if (stored.isRevoked()) {
            throw reuseDetected(userId, ipAddress, userAgent);
        }
        if (stored.isExpired(now)) {
            throw new TokenExpiredException("Refresh token telah kedaluwarsa");
        }

        Optional<String> redisHash = refreshTokenStore.find(userId, tokenId);
        if (redisHash.isEmpty() || !redisHash.get().equals(tokenHash)) {
            throw reuseDetected(userId, ipAddress, userAgent);
        }

        User user = stored.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED) {
            revokeAll(userId, now);
            throw new AccountSuspendedException();
        }

        stored.revoke(now);
        refreshTokenRepository.save(stored);
        refreshTokenStore.delete(userId, tokenId);

        LoginResponse response = issueTokens(user, deviceInfo);
        auditEventPublisher.publishAfterCommit(audit(
                user.getId(), primaryRole(user.roleNames()), "TOKEN_REFRESHED", ENTITY_TYPE, user.getId().toString(),
                null, Map.of("rotatedTokenId", tokenId.toString()), ipAddress, userAgent, "SUCCESS"
        ));
        log.info("Refresh token berhasil dirotasi userId={}", user.getId());
        return response;
    }

    @Override
    @Transactional
    public void logout(JwtPrincipal principal, String ipAddress, String userAgent) {
        Duration remaining = Duration.between(Instant.now(), principal.expiresAt());
        tokenBlacklistService.blacklist(principal.jti(), remaining);

        Instant now = Instant.now();
        refreshTokenRepository.revokeAllByUserId(principal.userId(), now);
        refreshTokenStore.deleteAllForUser(principal.userId());

        auditEventPublisher.publishAfterCommit(audit(
                principal.userId(), primaryRole(principal.roles()), "LOGOUT", ENTITY_TYPE,
                principal.userId().toString(), null, null, ipAddress, userAgent, "SUCCESS"
        ));
        log.info("Logout berhasil userId={}", principal.userId());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userById", key = "#userId")
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan"));
        return UserResponse.from(user);
    }

    private LoginResponse issueTokens(User user, String deviceInfo) {
        Set<String> roles = user.roleNames();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), roles);

        UUID tokenId = UUID.randomUUID();
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tokenId);
        String tokenHash = TokenHasher.hash(refreshToken);
        Instant expiresAt = Instant.now().plusSeconds(jwtUtil.getRefreshTokenTtlSeconds());

        RefreshToken entity = RefreshToken.create(user, tokenId, tokenHash, deviceInfo, expiresAt);
        refreshTokenRepository.save(entity);
        refreshTokenStore.save(user.getId(), tokenId, tokenHash,
                Duration.ofSeconds(jwtUtil.getRefreshTokenTtlSeconds()));

        return new LoginResponse(accessToken, refreshToken, TOKEN_TYPE, jwtUtil.getAccessTokenTtlSeconds());
    }

    private TokenRevokedException reuseDetected(UUID userId, String ipAddress, String userAgent) {
        revokeAll(userId, Instant.now());
        auditEventPublisher.publish(audit(
                userId, ANONYMOUS_ROLE, "REFRESH_REUSE_DETECTED", ENTITY_TYPE, userId.toString(),
                null, Map.of("reason", "REFRESH_TOKEN_REUSE"), ipAddress, userAgent, "FAILURE"
        ));
        log.warn("Terdeteksi penggunaan ulang refresh token userId={}; seluruh sesi dicabut", userId);
        return new TokenRevokedException("Refresh token tidak valid atau sudah digunakan, silakan login kembali");
    }

    private void revokeAll(UUID userId, Instant revokedAt) {
        refreshTokenRepository.revokeAllByUserId(userId, revokedAt);
        refreshTokenStore.deleteAllForUser(userId);
    }

    private AuditPayload audit(UUID actorId, String actorRole, String action, String entityId,
                               Map<String, Object> oldValue, Map<String, Object> newValue,
                               String ipAddress, String userAgent, String result) {
        return audit(actorId, actorRole, action, ENTITY_TYPE, entityId, oldValue, newValue, ipAddress, userAgent, result);
    }

    private AuditPayload audit(UUID actorId, String actorRole, String action, String entityType, String entityId,
                               Map<String, Object> oldValue, Map<String, Object> newValue,
                               String ipAddress, String userAgent, String result) {
        return new AuditPayload(
                actorId == null ? null : actorId.toString(),
                actorRole,
                action,
                entityType,
                entityId,
                oldValue,
                newValue,
                ipAddress,
                userAgent,
                result,
                null
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String primaryRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return ANONYMOUS_ROLE;
        }
        return roles.stream().sorted().findFirst().orElse(ANONYMOUS_ROLE);
    }
}
