package com.koperasi.reporting.repository;

import com.koperasi.reporting.domain.dto.LoanReportRow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportQueryRepository {

    private static final String LOAN_FROM = """
            FROM loan_schema.loan_applications la
            LEFT JOIN auth_schema.users u ON u.id = la.borrower_id
            LEFT JOIN auth_schema.borrower_profiles bp ON bp.user_id = la.borrower_id
            """;

    private static final String LOAN_FILTERS = """
            WHERE (CAST(:fromDate AS date) IS NULL OR la.submitted_at >= CAST(:fromDate AS date))
              AND (CAST(:toDate AS date) IS NULL OR la.submitted_at < CAST(:toDate AS date) + INTERVAL '1 day')
              AND (CAST(:status AS varchar) IS NULL OR la.status = :status)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReportQueryRepository(
            @Qualifier("reportJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> summary(LocalDate from, LocalDate to) {
        String sql = """
                SELECT COUNT(*) AS total_loans,
                       COALESCE(SUM(CASE WHEN la.status IN ('DISBURSED', 'ACTIVE', 'COMPLETED', 'DEFAULT')
                                         THEN la.amount ELSE 0 END), 0) AS total_disbursed,
                       COALESCE(SUM(CASE WHEN la.status IN ('DISBURSED', 'ACTIVE')
                                         THEN la.amount ELSE 0 END), 0) AS active_amount,
                       COALESCE(SUM(CASE WHEN la.disbursed_at::date = CURRENT_DATE
                                         THEN la.amount ELSE 0 END), 0) AS disbursed_today_amount,
                       COALESCE(SUM(CASE WHEN la.status = 'DEFAULT'
                                         THEN la.amount ELSE 0 END), 0) AS defaulted_amount,
                       COUNT(*) FILTER (WHERE la.status IN ('DISBURSED', 'ACTIVE')) AS active_loans,
                       COUNT(*) FILTER (WHERE la.status = 'COMPLETED') AS completed_loans,
                       COUNT(*) FILTER (WHERE la.status = 'DEFAULT') AS defaulted_loans,
                       COUNT(*) FILTER (WHERE la.status IN ('PENDING', 'SCORING', 'UNDER_REVIEW')) AS pending_review
                """ + LOAN_FROM + LOAN_FILTERS;
        return jdbcTemplate.queryForMap(sql, filters(from, to, null));
    }

    public Map<String, Long> countsByStatus(LocalDate from, LocalDate to) {
        String sql = """
                SELECT la.status, COUNT(*) AS total
                """ + LOAN_FROM + LOAN_FILTERS + " GROUP BY la.status";
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, filters(from, to, null), rs -> {
            result.put(rs.getString("status"), rs.getLong("total"));
        });
        return result;
    }

    public List<Map<String, Object>> monthlyDisbursement(LocalDate from, LocalDate to) {
        String sql = """
                SELECT to_char(date_trunc('month', la.disbursed_at), 'YYYY-MM') AS period,
                       COALESCE(SUM(la.amount), 0) AS total
                """ + LOAN_FROM + """
                WHERE la.disbursed_at IS NOT NULL
                  AND la.disbursed_at >= date_trunc('month', CURRENT_DATE) - INTERVAL '5 months'
                  AND (CAST(:fromDate AS date) IS NULL OR la.submitted_at >= CAST(:fromDate AS date))
                  AND (CAST(:toDate AS date) IS NULL OR la.submitted_at < CAST(:toDate AS date) + INTERVAL '1 day')
                GROUP BY 1
                ORDER BY 1
                """;
        return jdbcTemplate.queryForList(sql, filters(from, to, null));
    }

    public List<Map<String, Object>> monthlyDecisions(LocalDate from, LocalDate to) {
        String sql = """
                SELECT to_char(date_trunc('month', la.reviewed_at), 'YYYY-MM') AS period,
                       COUNT(*) FILTER (WHERE la.status IN
                           ('APPROVED', 'DISBURSED', 'ACTIVE', 'COMPLETED', 'DEFAULT')) AS approved,
                       COUNT(*) FILTER (WHERE la.status = 'REJECTED') AS rejected
                """ + LOAN_FROM + """
                WHERE la.reviewed_at IS NOT NULL
                  AND la.reviewed_at >= date_trunc('month', CURRENT_DATE) - INTERVAL '5 months'
                  AND (CAST(:fromDate AS date) IS NULL OR la.submitted_at >= CAST(:fromDate AS date))
                  AND (CAST(:toDate AS date) IS NULL OR la.submitted_at < CAST(:toDate AS date) + INTERVAL '1 day')
                GROUP BY 1
                ORDER BY 1
                """;
        return jdbcTemplate.queryForList(sql, filters(from, to, null));
    }

    public List<Map<String, Object>> riskDistribution(LocalDate from, LocalDate to) {
        String sql = """
                SELECT cs.risk_category, COUNT(*) AS total
                FROM loan_schema.credit_scores cs
                JOIN loan_schema.loan_applications la ON la.id = cs.loan_application_id
                """ + LOAN_FILTERS + """
                  AND la.status IN ('DISBURSED', 'ACTIVE', 'DEFAULT')
                GROUP BY cs.risk_category
                """;
        return jdbcTemplate.queryForList(sql, filters(from, to, null));
    }

    public List<LoanReportRow> findLoans(LocalDate from, LocalDate to, String status,
                                         int page, int size) {
        String sql = """
                SELECT la.id,
                       COALESCE(bp.full_name, split_part(u.email, '@', 1)) AS borrower_name,
                       la.amount,
                       la.status,
                       la.disbursed_at,
                       COALESCE((SELECT SUM(li.amount)
                                 FROM loan_schema.loan_installments li
                                 WHERE li.loan_application_id = la.id
                                   AND li.status <> 'PAID'), 0) AS outstanding_amount
                """ + LOAN_FROM + LOAN_FILTERS + """
                ORDER BY la.submitted_at DESC
                LIMIT :limit OFFSET :offset
                """;
        MapSqlParameterSource params = filters(from, to, status)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new LoanReportRow(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("borrower_name"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("disbursed_at")),
                rs.getBigDecimal("outstanding_amount")
        ));
    }

    public long countLoans(LocalDate from, LocalDate to, String status) {
        String sql = "SELECT COUNT(*) " + LOAN_FROM + LOAN_FILTERS;
        Long total = jdbcTemplate.queryForObject(sql, filters(from, to, status), Long.class);
        return total == null ? 0 : total;
    }

    public List<LoanReportRow> findAllLoans(LocalDate from, LocalDate to, String status) {
        String sql = """
                SELECT la.id,
                       COALESCE(bp.full_name, split_part(u.email, '@', 1)) AS borrower_name,
                       la.amount,
                       la.status,
                       la.disbursed_at,
                       COALESCE((SELECT SUM(li.amount)
                                 FROM loan_schema.loan_installments li
                                 WHERE li.loan_application_id = la.id
                                   AND li.status <> 'PAID'), 0) AS outstanding_amount
                """ + LOAN_FROM + LOAN_FILTERS + " ORDER BY la.submitted_at DESC";
        return jdbcTemplate.query(sql, filters(from, to, status), (rs, rowNum) -> new LoanReportRow(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("borrower_name"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("disbursed_at")),
                rs.getBigDecimal("outstanding_amount")
        ));
    }

    private MapSqlParameterSource filters(LocalDate from, LocalDate to, String status) {
        return new MapSqlParameterSource()
                .addValue("fromDate", from == null ? null : from.toString())
                .addValue("toDate", to == null ? null : to.toString())
                .addValue("status", status == null || status.isBlank() ? null : status.toUpperCase());
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
