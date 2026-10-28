package com.koperasi.auth.service;

import com.koperasi.auth.domain.dto.CreateUserRequest;
import com.koperasi.auth.domain.dto.PagedUserResponse;
import com.koperasi.auth.domain.dto.UpdateUserRequest;
import com.koperasi.auth.domain.dto.UserResponse;
import com.koperasi.auth.domain.entity.Role;
import com.koperasi.auth.domain.entity.User;
import com.koperasi.auth.domain.entity.UserRole;
import com.koperasi.auth.domain.enums.RoleName;
import com.koperasi.auth.domain.enums.UserStatus;
import com.koperasi.auth.exception.ResourceNotFoundException;
import com.koperasi.auth.exception.UserAlreadyExistsException;
import com.koperasi.auth.repository.RefreshTokenRepository;
import com.koperasi.auth.repository.RoleRepository;
import com.koperasi.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PagedUserResponse list(String search, RoleName role, UserStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.search(blankToNull(search), role, status, pageable);
        return new PagedUserResponse(
                result.getContent().stream().map(UserResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException();
        }

        User user = User.create(
                email,
                passwordEncoder.encode(request.password()),
                blankToNull(request.phone()),
                request.status() == null ? UserStatus.ACTIVE : request.status()
        );
        user.addRole(findRole(request.role()));
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    @CacheEvict(cacheNames = "userById", key = "#userId")
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        User user = findUser(userId);

        if (request.phone() != null) {
            user.setPhone(blankToNull(request.phone()));
        }
        if (request.status() != null) {
            user.setStatus(request.status());
            if (request.status() == UserStatus.SUSPENDED) {
                refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
            }
        }
        if (request.role() != null) {
            applyRole(user, request.role());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        }

        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    @CacheEvict(cacheNames = "userById", key = "#userId")
    public void deactivate(UUID userId) {
        User user = findUser(userId);
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    private void applyRole(User user, RoleName roleName) {
        Role role = findRole(roleName);
        boolean alreadyHasRole = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .anyMatch(existing -> existing.getName() == roleName);
        if (alreadyHasRole) {
            return;
        }
        user.getUserRoles().clear();
        user.addRole(role);
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Peran tidak ditemukan: " + roleName));
    }

    private User findUser(UUID userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan"));
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
