package com.koperasi.loan.controller;

import com.koperasi.loan.domain.dto.ApiResponse;
import com.koperasi.loan.domain.dto.LoanInstallmentDetailsResponse;
import com.koperasi.loan.domain.dto.MarkInstallmentPaidRequest;
import com.koperasi.loan.exception.ForbiddenOperationException;
import com.koperasi.loan.service.LoanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans/internal/installments")
public class InternalLoanController {

    private final LoanService loanService;
    private final String expectedToken;

    public InternalLoanController(
            LoanService loanService,
            @Value("${app.internal.callback-token:}") String expectedToken
    ) {
        this.loanService = loanService;
        this.expectedToken = expectedToken;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanInstallmentDetailsResponse>> getInstallment(
            @PathVariable UUID id,
            @RequestHeader(name = "X-Internal-Token", required = false) String token
    ) {
        verifyToken(token);
        return ResponseEntity.ok(ApiResponse.ok(loanService.getInstallmentDetails(id), "OK"));
    }

    @PostMapping("/{id}/paid")
    public ResponseEntity<ApiResponse<LoanInstallmentDetailsResponse>> markPaid(
            @PathVariable UUID id,
            @RequestHeader(name = "X-Internal-Token", required = false) String token,
            @RequestBody(required = false) MarkInstallmentPaidRequest request
    ) {
        verifyToken(token);
        Instant paidAt = request == null ? null : request.paidAt();
        return ResponseEntity.ok(ApiResponse.ok(loanService.markInstallmentPaid(id, paidAt), "Cicilan ditandai lunas"));
    }

    private void verifyToken(String token) {
        if (expectedToken == null || expectedToken.isBlank() || token == null
                || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                expectedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenOperationException("Token internal tidak valid");
        }
    }
}
