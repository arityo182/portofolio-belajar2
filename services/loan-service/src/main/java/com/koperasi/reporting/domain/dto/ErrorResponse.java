package com.koperasi.reporting.domain.dto;

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
    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(false, code, message, details == null ? List.of() : details,
                Instant.now().toString());
    }
}
