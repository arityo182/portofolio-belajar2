package com.koperasi.loan.controller;

import com.koperasi.loan.domain.dto.ApiResponse;
import com.koperasi.loan.domain.dto.LoanProductRequest;
import com.koperasi.loan.domain.dto.LoanProductResponse;
import com.koperasi.loan.service.LoanProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans/admin/products")
@RequiredArgsConstructor
public class AdminLoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LoanProductResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(loanProductService.listAllProducts(), "OK"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanProductResponse>> create(
            @Valid @RequestBody LoanProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(loanProductService.create(request), "Produk dibuat"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanProductResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody LoanProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(loanProductService.update(id, request), "Produk diperbarui"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        loanProductService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
