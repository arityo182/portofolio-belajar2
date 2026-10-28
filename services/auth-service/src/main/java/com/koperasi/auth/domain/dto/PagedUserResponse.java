package com.koperasi.auth.domain.dto;

import java.util.List;

public record PagedUserResponse(
        List<UserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
