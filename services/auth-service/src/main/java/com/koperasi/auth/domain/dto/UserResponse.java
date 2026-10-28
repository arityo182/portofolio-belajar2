package com.koperasi.auth.domain.dto;

import com.koperasi.auth.domain.entity.User;
import com.koperasi.auth.domain.enums.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String email,
        String phone,
        UserStatus status,
        Set<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.roleNames(),
                user.getCreatedAt()
        );
    }
}
