package com.koperasi.loan.domain.entity;

import com.koperasi.loan.domain.enums.ApprovalRecommendation;
import com.koperasi.loan.domain.enums.DisbursementStatus;
import com.koperasi.loan.domain.enums.InstallmentStatus;
import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.domain.enums.PaymentGateway;
import com.koperasi.loan.domain.enums.RiskCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEntityTest {

    @Test
    void loanProduct_supportsOnlyAmountAndTenorWithinRange() {
        LoanProduct product = product();

        assertThat(product.supports(new BigDecimal("10000000.00"), 12)).isTrue();
        assertThat(product.supports(new BigDecimal("999999.00"), 12)).isFalse();
        assertThat(product.supports(new BigDecimal("50000001.00"), 12)).isFalse();
        assertThat(product.supports(new BigDecimal("10000000.00"), 2)).isFalse();
        assertThat(product.supports(new BigDecimal("10000000.00"), 25)).isFalse();
        assertThat(product.supports(null, 12)).isFalse();
    }

    @Test
    void loanApplication_createInitializesPendingApplicationAndPersistableState() {
        LoanProduct product = product();
        UUID borrowerId = UUID.randomUUID();

        LoanApplication application = LoanApplication.create(
                borrowerId, product, new BigDecimal("10000000.00"), 12, "Modal usaha", "idem-1");

        assertThat(application.getId()).isNotNull();
        assertThat(application.getBorrowerId()).isEqualTo(borrowerId);
        assertThat(application.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(application.getSubmittedAt()).isNotNull();
        assertThat(application.isNew()).isTrue();

        application.onCreate();

        assertThat(application.getCreatedAt()).isNotNull();
        assertThat(application.getUpdatedAt()).isNotNull();
        application.markNotNew();
        assertThat(application.isNew()).isFalse();
    }

    @Test
    void creditScore_createAndApplyResultPopulatesScoringFields() {
        LoanApplication application = LoanApplication.create(
                UUID.randomUUID(), product(), new BigDecimal("10000000.00"), 12, "Modal usaha", "idem-1");
        CreditScore score = CreditScore.create(application, application.getBorrowerId());

        score.applyResult(
                new BigDecimal("78.50"),
                RiskCategory.MEDIUM,
                ApprovalRecommendation.REVIEW,
                Map.of("ratio", 0.8),
                "v2.3.1",
                420,
                null
        );
        score.onCreate();

        assertThat(score.getId()).isNotNull();
        assertThat(score.getScore()).isEqualByComparingTo("78.50");
        assertThat(score.getRiskCategory()).isEqualTo(RiskCategory.MEDIUM);
        assertThat(score.getApprovalRecommendation()).isEqualTo(ApprovalRecommendation.REVIEW);
        assertThat(score.getFeaturesJson()).containsEntry("ratio", 0.8);
        assertThat(score.getScoredAt()).isNotNull();
    }

    @Test
    void loanDisbursement_createInitializesPendingDisbursement() {
        LoanApplication application = LoanApplication.create(
                UUID.randomUUID(), product(), new BigDecimal("10000000.00"), 12, "Modal usaha", "idem-1");

        LoanDisbursement disbursement = LoanDisbursement.create(
                application, application.getAmount(), PaymentGateway.MIDTRANS, "disb-1");
        disbursement.onCreate();

        assertThat(disbursement.getId()).isNotNull();
        assertThat(disbursement.getStatus()).isEqualTo(DisbursementStatus.PENDING);
        assertThat(disbursement.getPaymentGateway()).isEqualTo(PaymentGateway.MIDTRANS);
        assertThat(disbursement.getCreatedAt()).isNotNull();
    }

    @Test
    void loanInstallment_createInitializesPendingInstallment() {
        LoanApplication application = LoanApplication.create(
                UUID.randomUUID(), product(), new BigDecimal("12000000.00"), 12, "Modal usaha", "idem-1");

        LoanInstallment installment = LoanInstallment.create(
                application, 1, LocalDate.now().plusMonths(1),
                new BigDecimal("1180000.00"), new BigDecimal("1000000.00"), new BigDecimal("180000.00"));
        installment.onCreate();

        assertThat(installment.getId()).isNotNull();
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PENDING);
        assertThat(installment.getCreatedAt()).isNotNull();
    }

    private LoanProduct product() {
        LoanProduct product = new LoanProduct();
        product.setId(1);
        product.setName("Modal Usaha Mikro");
        product.setMinAmount(new BigDecimal("1000000.00"));
        product.setMaxAmount(new BigDecimal("50000000.00"));
        product.setInterestRateAnnual(new BigDecimal("18.00"));
        product.setMinTenorMonths(3);
        product.setMaxTenorMonths(24);
        product.setActive(true);
        return product;
    }
}
