package com.koperasi.reporting.domain.dto;

import org.slf4j.MDC;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String timestamp,
        String traceId
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now().toString(), MDC.get("traceId"));
    }
}
