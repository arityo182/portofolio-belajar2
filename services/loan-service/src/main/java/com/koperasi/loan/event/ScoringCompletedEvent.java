package com.koperasi.loan.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.koperasi.loan.domain.enums.ApprovalRecommendation;
import com.koperasi.loan.domain.enums.RiskCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoringCompletedEvent(
        String eventId,
        String eventType,
        String timestamp,
        String traceId,
        String version,
        ScoringPayload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScoringPayload(
            UUID loanApplicationId,
            UUID borrowerId,
            BigDecimal riskScore,
            RiskCategory riskCategory,
            ApprovalRecommendation approvalRecommendation,
            BigDecimal confidence,
            Map<String, Object> features,
            String modelVersion,
            Integer processingTimeMs,
            String scoringStatus,
            Instant scoredAt
    ) {
    }
}
