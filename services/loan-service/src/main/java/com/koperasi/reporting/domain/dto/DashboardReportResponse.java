package com.koperasi.reporting.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardReportResponse(
        long totalLoans,
        BigDecimal totalDisbursed,
        BigDecimal activeAmount,
        BigDecimal disbursedTodayAmount,
        BigDecimal defaultedAmount,
        long activeLoans,
        long completedLoans,
        long defaultedLoans,
        long pendingReview,
        BigDecimal nplRatio,
        Map<String, Long> loansByStatus,
        List<MonthlyAmount> monthlyDisbursement,
        List<MonthlyDecisions> monthlyDecisions,
        List<RiskBucket> riskDistribution
) {

    public record MonthlyAmount(String key, String label, BigDecimal disbursed) {
    }

    public record MonthlyDecisions(String key, String label, long approved, long rejected) {
    }

    public record RiskBucket(String name, String key, long value) {
    }
}
