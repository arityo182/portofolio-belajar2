package com.koperasi.loan.controller;

import com.koperasi.loan.domain.dto.ApiResponse;
import com.koperasi.loan.event.ScoringCompletedEvent;
import com.koperasi.loan.exception.ForbiddenOperationException;
import com.koperasi.loan.service.LoanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/loans/internal/scoring")
public class InternalScoringController {

    private final LoanService loanService;
    private final String expectedToken;

    public InternalScoringController(
            LoanService loanService,
            @Value("${app.internal.callback-token:}") String expectedToken
    ) {
        this.loanService = loanService;
        this.expectedToken = expectedToken;
    }

    @PostMapping("/completed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receiveScoringCompleted(
            @RequestHeader(name = "X-Internal-Token", required = false) String token,
            @RequestBody ScoringCompletedEvent event
    ) {
        if (expectedToken == null || expectedToken.isBlank() || !matches(token, expectedToken)) {
            throw new ForbiddenOperationException("Token internal tidak valid");
        }
        loanService.handleScoringCompleted(event);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("processed", true), "Hasil scoring diproses"));
    }

    private boolean matches(String provided, String expected) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }
}
