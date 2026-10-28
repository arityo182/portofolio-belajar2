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
import com.koperasi.loan.domain.enums.ApprovalRecommendation;
import com.koperasi.loan.domain.enums.InstallmentStatus;
import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.domain.enums.RiskCategory;
import com.koperasi.loan.security.JwtPrincipal;
import com.koperasi.loan.service.LoanProductService;
import com.koperasi.loan.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @Mock
    private LoanProductService loanProductService;

    private LoanController loanController;
    private JwtPrincipal principal;

    @BeforeEach
    void setUp() {
        loanController = new LoanController(loanService, loanProductService);
        principal = new JwtPrincipal(UUID.randomUUID(), "user@example.id", Set.of("BORROWER"),
                "jti", Instant.now().plusSeconds(900));
    }

    @Test
    void getProducts_returnsProductList() {
        LoanProductResponse product = productResponse();
        when(loanProductService.listActiveProducts()).thenReturn(List.of(product));

        ResponseEntity<ApiResponse<List<LoanProductResponse>>> response = loanController.getProducts();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).containsExactly(product);
    }

    @Test
    void apply_returnsCreatedResponse() {
        LoanApplicationResponse application = applicationResponse();
        when(loanService.apply(any(CreateLoanApplicationRequest.class), eq("idem-1"), eq(principal)))
                .thenReturn(application);
        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                1, new BigDecimal("10000000.00"), 12, "Modal usaha");

        ResponseEntity<ApiResponse<LoanApplicationResponse>> response = loanController.apply(request, "idem-1", principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(application);
        assertThat(response.getBody().message()).contains("Pengajuan diterima");
        verify(loanService).apply(request, "idem-1", principal);
    }

    @Test
    void listApplications_returnsPage() {
        PageResponse<LoanApplicationSummaryResponse> page = new PageResponse<>(
                List.of(summaryResponse()), 0, 20, 1, 1);
        when(loanService.list(principal, LoanStatus.PENDING, 0, 20)).thenReturn(page);

        ResponseEntity<ApiResponse<PageResponse<LoanApplicationSummaryResponse>>> response =
                loanController.listApplications(LoanStatus.PENDING, 0, 20, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().totalElements()).isEqualTo(1);
    }

    @Test
    void getApplication_returnsDetail() {
        LoanApplicationResponse application = applicationResponse();
        when(loanService.getById(application.loanId(), principal)).thenReturn(application);

        ResponseEntity<ApiResponse<LoanApplicationResponse>> response =
                loanController.getApplication(application.loanId(), principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().loanId()).isEqualTo(application.loanId());
    }

    @Test
    void updateStatus_returnsUpdatedApplication() {
        LoanApplicationResponse application = applicationResponse();
        UpdateLoanStatusRequest request = new UpdateLoanStatusRequest(
                LoanStatus.APPROVED, null, false, null, 1L);
        when(loanService.updateStatus(application.loanId(), request, principal)).thenReturn(application);

        ResponseEntity<ApiResponse<LoanApplicationResponse>> response =
                loanController.updateStatus(application.loanId(), request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Status pinjaman diperbarui");
    }

    @Test
    void getInstallments_returnsSchedule() {
        UUID loanId = UUID.randomUUID();
        LoanInstallmentResponse installment = new LoanInstallmentResponse(
                UUID.randomUUID(), 1, LocalDate.now().plusMonths(1),
                new BigDecimal("1180000.00"), new BigDecimal("1000000.00"),
                new BigDecimal("180000.00"), InstallmentStatus.PENDING, null);
        when(loanService.getInstallments(loanId, principal)).thenReturn(List.of(installment));

        ResponseEntity<ApiResponse<List<LoanInstallmentResponse>>> response =
                loanController.getInstallments(loanId, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).containsExactly(installment);
    }

    @Test
    void getScore_returnsCreditScore() {
        UUID loanId = UUID.randomUUID();
        CreditScoreResponse score = new CreditScoreResponse(
                UUID.randomUUID(), loanId, UUID.randomUUID(), new BigDecimal("78.50"),
                RiskCategory.MEDIUM, ApprovalRecommendation.REVIEW, Map.of("ratio", 0.8),
                "v2.3.1", 420, Instant.now());
        when(loanService.getScore(loanId, principal)).thenReturn(score);

        ResponseEntity<ApiResponse<CreditScoreResponse>> response = loanController.getScore(loanId, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().score()).isEqualByComparingTo("78.50");
    }

    private LoanProductResponse productResponse() {
        return new LoanProductResponse(1, "Modal Usaha Mikro", new BigDecimal("1000000.00"),
                new BigDecimal("50000000.00"), new BigDecimal("18.00"), 3, 24, true);
    }

    private LoanApplicationSummaryResponse summaryResponse() {
        return new LoanApplicationSummaryResponse(UUID.randomUUID(), principal.userId(), 1,
                "Modal Usaha Mikro", new BigDecimal("10000000.00"), 12, LoanStatus.PENDING,
                null, Instant.now(), null, null);
    }

    private LoanApplicationResponse applicationResponse() {
        return new LoanApplicationResponse(UUID.randomUUID(), principal.userId(), 1, "Modal Usaha Mikro",
                new BigDecimal("10000000.00"), 12, "Modal usaha", LoanStatus.PENDING, null, null, null,
                Instant.now(), null, null, null, 0L, Instant.now());
    }
}
