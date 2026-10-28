package com.koperasi.auth.domain.dto;

import com.koperasi.auth.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        List<String> details,
        String timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode, String message, List<String> details) {
        return new ErrorResponse(false, errorCode.name(), message, details == null ? List.of() : details, Instant.now().toString());
    }

    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(false, code, message, details == null ? List.of() : details, Instant.now().toString());
    }
}
