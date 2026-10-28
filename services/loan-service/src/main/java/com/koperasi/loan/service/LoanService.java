package com.koperasi.loan.service;

import com.koperasi.loan.domain.dto.CreateLoanApplicationRequest;
import com.koperasi.loan.domain.dto.CreditScoreResponse;
import com.koperasi.loan.domain.dto.LoanApplicationOwnerResponse;
import com.koperasi.loan.domain.dto.LoanApplicationResponse;
import com.koperasi.loan.domain.dto.LoanApplicationSummaryResponse;
import com.koperasi.loan.domain.dto.LoanInstallmentDetailsResponse;
import com.koperasi.loan.domain.dto.LoanInstallmentResponse;
import com.koperasi.loan.domain.dto.PageResponse;
import com.koperasi.loan.domain.dto.UpdateLoanStatusRequest;
import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.event.ScoringCompletedEvent;
import com.koperasi.loan.security.JwtPrincipal;

import java.util.List;
import java.util.UUID;

public interface LoanService {

    LoanApplicationResponse apply(CreateLoanApplicationRequest request, String idempotencyKey, JwtPrincipal principal);

    PageResponse<LoanApplicationSummaryResponse> list(JwtPrincipal principal, LoanStatus status, int page, int size);

    LoanApplicationResponse getById(UUID loanId, JwtPrincipal principal);

    LoanApplicationResponse updateStatus(UUID loanId, UpdateLoanStatusRequest request, JwtPrincipal reviewer);

    LoanApplicationResponse disburse(UUID loanId);

    LoanApplicationResponse markDisbursed(UUID loanId, java.time.Instant disbursedAt);

    void markDefaulted(UUID loanId, int daysOverdue);

    List<LoanInstallmentResponse> getInstallments(UUID loanId, JwtPrincipal principal);

    CreditScoreResponse getScore(UUID loanId, JwtPrincipal principal);

    LoanApplicationOwnerResponse getApplicationOwner(UUID loanApplicationId);

    LoanInstallmentDetailsResponse getInstallmentDetails(UUID installmentId);

    LoanInstallmentDetailsResponse markInstallmentPaid(UUID installmentId, java.time.Instant paidAt);

    void handleScoringCompleted(ScoringCompletedEvent event);
}
