package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.entity.CreditScore;
import com.koperasi.loan.domain.enums.ApprovalRecommendation;
import com.koperasi.loan.domain.enums.RiskCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreditScoreResponse(
        UUID scoreId,
        UUID loanApplicationId,
        UUID borrowerId,
        BigDecimal score,
        RiskCategory riskCategory,
        ApprovalRecommendation approvalRecommendation,
        Map<String, Object> features,
        String modelVersion,
        Integer processingTimeMs,
        Instant scoredAt
) {
    public static CreditScoreResponse from(CreditScore creditScore) {
        return new CreditScoreResponse(
                creditScore.getId(),
                creditScore.getLoanApplication().getId(),
                creditScore.getBorrowerId(),
                creditScore.getScore(),
                creditScore.getRiskCategory(),
                creditScore.getApprovalRecommendation(),
                creditScore.getFeaturesJson(),
                creditScore.getModelVersion(),
                creditScore.getProcessingTimeMs(),
                creditScore.getScoredAt()
        );
    }
}
