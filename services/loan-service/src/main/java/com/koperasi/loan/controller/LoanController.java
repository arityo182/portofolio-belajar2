package com.koperasi.loan.controller;

import com.koperasi.loan.domain.dto.ApiResponse;
import com.koperasi.loan.domain.dto.CreateLoanApplicationRequest;
import com.koperasi.loan.domain.dto.CreditScoreResponse;
import com.koperasi.loan.domain.dto.LoanApplicationResponse;
import com.koperasi.loan.domain.dto.LoanApplicationSummaryResponse;
import com.koperasi.loan.domain.dto.LoanInstallmentResponse;
import com.koperasi.loan.domain.dto.LoanProductResponse;
import com.koperasi.loan.domain.dto.PageResponse;
import com.koperasi.loan.domain.dto.UpdateLoanStatusRequest;
import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.security.JwtPrincipal;
import com.koperasi.loan.service.LoanProductService;
import com.koperasi.loan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanProductService loanProductService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<LoanProductResponse>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.ok(loanProductService.listActiveProducts(), "OK"));
    }

    @PostMapping("/applications")
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> apply(
            @Valid @RequestBody CreateLoanApplicationRequest request,
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal JwtPrincipal principal) {
        LoanApplicationResponse response = loanService.apply(request, idempotencyKey, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Pengajuan diterima dan masuk antrian scoring"));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('BORROWER','REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<LoanApplicationSummaryResponse>>> listApplications(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtPrincipal principal) {
        PageResponse<LoanApplicationSummaryResponse> response = loanService.list(principal, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response, "OK"));
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('BORROWER','REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(loanService.getById(id, principal), "OK"));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLoanStatusRequest request,
            @AuthenticationPrincipal JwtPrincipal reviewer) {
        LoanApplicationResponse response = loanService.updateStatus(id, request, reviewer);
        return ResponseEntity.ok(ApiResponse.ok(response, "Status pinjaman diperbarui"));
    }

    @GetMapping("/applications/{id}/installments")
    @PreAuthorize("hasAnyRole('BORROWER','REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<List<LoanInstallmentResponse>>> getInstallments(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(loanService.getInstallments(id, principal), "OK"));
    }

    @GetMapping("/applications/{id}/score")
    @PreAuthorize("hasAnyRole('BORROWER','REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditScoreResponse>> getScore(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(loanService.getScore(id, principal), "OK"));
    }
}
