package com.koperasi.loan.domain.entity;

import com.koperasi.loan.domain.enums.ApprovalRecommendation;
import com.koperasi.loan.domain.enums.RiskCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "credit_scores", schema = "loan_schema")
@Getter
@Setter
@NoArgsConstructor
public class CreditScore {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    @Column(name = "borrower_id", nullable = false)
    private UUID borrowerId;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false, length = 20)
    private RiskCategory riskCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_recommendation", nullable = false, length = 20)
    private ApprovalRecommendation approvalRecommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> featuresJson = new HashMap<>();

    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    public static CreditScore create(LoanApplication application, UUID borrowerId) {
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setLoanApplication(application);
        creditScore.setBorrowerId(borrowerId);
        creditScore.setFeaturesJson(new HashMap<>());
        return creditScore;
    }

    public void applyResult(BigDecimal score, RiskCategory riskCategory,
                            ApprovalRecommendation approvalRecommendation,
                            Map<String, Object> features, String modelVersion,
                            Integer processingTimeMs, Instant scoredAt) {
        this.score = score;
        this.riskCategory = riskCategory;
        this.approvalRecommendation = approvalRecommendation;
        this.featuresJson = features == null ? new HashMap<>() : new HashMap<>(features);
        this.modelVersion = modelVersion;
        this.processingTimeMs = processingTimeMs;
        this.scoredAt = scoredAt == null ? Instant.now() : scoredAt;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (scoredAt == null) {
            scoredAt = Instant.now();
        }
        if (featuresJson == null) {
            featuresJson = new HashMap<>();
        }
    }
}
