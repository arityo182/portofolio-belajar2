package com.koperasi.payment.controller;

import com.koperasi.payment.domain.dto.ApiResponse;
import com.koperasi.payment.domain.dto.CreateDisbursementRequest;
import com.koperasi.payment.domain.dto.CreatePaymentRequest;
import com.koperasi.payment.domain.dto.DisbursementResponse;
import com.koperasi.payment.domain.dto.PaymentResponse;
import com.koperasi.payment.domain.dto.PaymentStatusResponse;
import com.koperasi.payment.security.JwtPrincipal;
import com.koperasi.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/installments/{id}/pay")
    @PreAuthorize("hasRole('BORROWER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> pay(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal JwtPrincipal principal) {
        PaymentResponse response = paymentService.payInstallment(id, request, idempotencyKey, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Pembayaran diinisiasi, lanjutkan ke payment gateway"));
    }

    @GetMapping("/installments/{id}/status")
    @PreAuthorize("hasAnyRole('BORROWER','REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> status(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getInstallmentStatus(id, principal), "OK"));
    }

    @PostMapping("/disbursements")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ResponseEntity<ApiResponse<DisbursementResponse>> disburse(
            @Valid @RequestBody CreateDisbursementRequest request,
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal JwtPrincipal principal) {
        DisbursementResponse response = paymentService.initiateDisbursement(request, idempotencyKey, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Pencairan diinisiasi"));
    }
}
