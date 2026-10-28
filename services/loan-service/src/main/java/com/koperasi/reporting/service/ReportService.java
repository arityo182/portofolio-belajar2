package com.koperasi.reporting.service;

import com.koperasi.reporting.domain.dto.DashboardReportResponse;
import com.koperasi.reporting.domain.dto.LoanReportRow;
import com.koperasi.reporting.domain.dto.PageResponse;
import com.koperasi.reporting.repository.ReportQueryRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yy", Locale.forLanguageTag("id-ID"));
    private static final DateTimeFormatter PDF_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"));
    private static final ZoneId PDF_ZONE = ZoneId.of("Asia/Jakarta");
    private static final Locale PDF_LOCALE = Locale.forLanguageTag("id-ID");
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> STATUS_LABELS = Map.of(
            "PENDING", "Menunggu",
            "SCORING", "Penilaian",
            "UNDER_REVIEW", "Ditinjau",
            "APPROVED", "Disetujui",
            "REJECTED", "Ditolak",
            "DISBURSED", "Dicairkan",
            "ACTIVE", "Aktif",
            "COMPLETED", "Selesai",
            "DEFAULT", "Gagal bayar"
    );

    private final ReportQueryRepository repository;

    @Transactional(readOnly = true)
    public DashboardReportResponse dashboard(LocalDate from, LocalDate to) {
        Map<String, Object> summary = repository.summary(from, to);
        long totalLoans = longValue(summary.get("total_loans"));
        long activeLoans = longValue(summary.get("active_loans"));
        long completedLoans = longValue(summary.get("completed_loans"));
        long defaultedLoans = longValue(summary.get("defaulted_loans"));
        BigDecimal totalDisbursed = ReportQueryRepository.toBigDecimal(summary.get("total_disbursed"));
        BigDecimal activeAmount = ReportQueryRepository.toBigDecimal(summary.get("active_amount"));
        BigDecimal disbursedTodayAmount =
                ReportQueryRepository.toBigDecimal(summary.get("disbursed_today_amount"));
        BigDecimal defaultedAmount = ReportQueryRepository.toBigDecimal(summary.get("defaulted_amount"));
        long pendingReview = longValue(summary.get("pending_review"));
        BigDecimal nplRatio = totalLoans == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(defaultedLoans)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalLoans), 2, RoundingMode.HALF_UP);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (String status : List.of("PENDING", "SCORING", "UNDER_REVIEW", "APPROVED",
                "REJECTED", "DISBURSED", "ACTIVE", "COMPLETED", "DEFAULT")) {
            statusCounts.put(status, 0L);
        }
        repository.countsByStatus(from, to).forEach(statusCounts::put);

        return new DashboardReportResponse(
                totalLoans,
                totalDisbursed,
                activeAmount,
                disbursedTodayAmount,
                defaultedAmount,
                activeLoans,
                completedLoans,
                defaultedLoans,
                pendingReview,
                nplRatio,
                statusCounts,
                monthlyDisbursement(from, to),
                monthlyDecisions(from, to),
                riskDistribution(from, to)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanReportRow> loans(LocalDate from, LocalDate to, String status,
                                             int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        List<LoanReportRow> content = repository.findLoans(from, to, status, safePage, safeSize);
        long total = repository.countLoans(from, to, status);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public String exportCsv(LocalDate from, LocalDate to, String status) {
        List<LoanReportRow> rows = repository.findAllLoans(from, to, status);
        StringBuilder csv = new StringBuilder();
        csv.append("loanId,borrowerName,amount,status,statusLabel,disbursedAt,outstandingAmount\n");
        for (LoanReportRow row : rows) {
            csv.append(csvValue(row.loanId()))
                    .append(',').append(csvValue(row.borrowerName()))
                    .append(',').append(csvValue(row.amount()))
                    .append(',').append(csvValue(row.status()))
                    .append(',').append(csvValue(STATUS_LABELS.getOrDefault(row.status(), row.status())))
                    .append(',').append(csvValue(row.disbursedAt()))
                    .append(',').append(csvValue(row.outstandingAmount()))
                    .append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(LocalDate from, LocalDate to, String status) {
        List<LoanReportRow> rows = repository.findAllLoans(from, to, status);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            Font titleFont = new Font(Font.HELVETICA, 15, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 9);
            document.add(new Paragraph("Laporan Pinjaman - Koperasi Digital", titleFont));
            document.add(new Paragraph("Periode: " + (from == null ? "awal" : from)
                    + " s/d " + (to == null ? "sekarang" : to), bodyFont));
            document.add(new Paragraph("Total: " + rows.size() + " pinjaman", bodyFont));
            document.add(new Paragraph(" ", bodyFont));

            PdfPTable table = new PdfPTable(new float[]{3.2f, 1.4f, 1.9f, 1.6f, 2.4f, 2.1f});
            table.setWidthPercentage(100);
            for (String header : List.of("Peminjam", "ID", "Nominal", "Status",
                    "Dicairkan", "Sisa kewajiban")) {
                PdfPCell cell = new PdfPCell(new Phrase(header,
                        new Font(Font.HELVETICA, 9, Font.BOLD)));
                cell.setGrayFill(0.9f);
                table.addCell(cell);
            }
            for (LoanReportRow row : rows) {
                table.addCell(new Phrase(nullSafe(row.borrowerName()), bodyFont));
                table.addCell(new Phrase(shortId(row.loanId()), bodyFont));
                table.addCell(new Phrase(formatAmount(row.amount()), bodyFont));
                table.addCell(new Phrase(STATUS_LABELS.getOrDefault(row.status(), row.status()), bodyFont));
                table.addCell(new Phrase(formatPdfDate(row.disbursedAt()), bodyFont));
                table.addCell(new Phrase(formatAmount(row.outstandingAmount()), bodyFont));
            }
            document.add(table);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    private static String nullSafe(String value) {
        return value == null ? "-" : value;
    }

    private static String shortId(java.util.UUID loanId) {
        return loanId == null ? "-" : loanId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "0,00" : String.format(PDF_LOCALE, "%,.2f", amount);
    }

    private static String formatPdfDate(java.time.Instant instant) {
        return instant == null ? "-" : PDF_DATE.format(instant.atZone(PDF_ZONE));
    }

    private List<DashboardReportResponse.MonthlyAmount> monthlyDisbursement(LocalDate from, LocalDate to) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Map<String, Object> row : repository.monthlyDisbursement(from, to)) {
            totals.put(String.valueOf(row.get("period")),
                    ReportQueryRepository.toBigDecimal(row.get("total")));
        }
        List<DashboardReportResponse.MonthlyAmount> result = new ArrayList<>();
        for (YearMonth month : lastSixMonths()) {
            String key = month.format(MONTH_KEY);
            result.add(new DashboardReportResponse.MonthlyAmount(
                    key, month.format(MONTH_LABEL), totals.getOrDefault(key, BigDecimal.ZERO)));
        }
        return result;
    }

    private List<DashboardReportResponse.MonthlyDecisions> monthlyDecisions(LocalDate from, LocalDate to) {
        Map<String, long[]> totals = new LinkedHashMap<>();
        for (Map<String, Object> row : repository.monthlyDecisions(from, to)) {
            totals.put(String.valueOf(row.get("period")), new long[]{
                    longValue(row.get("approved")), longValue(row.get("rejected"))});
        }
        List<DashboardReportResponse.MonthlyDecisions> result = new ArrayList<>();
        for (YearMonth month : lastSixMonths()) {
            String key = month.format(MONTH_KEY);
            long[] counts = totals.getOrDefault(key, new long[]{0, 0});
            result.add(new DashboardReportResponse.MonthlyDecisions(
                    key, month.format(MONTH_LABEL), counts[0], counts[1]));
        }
        return result;
    }

    private List<DashboardReportResponse.RiskBucket> riskDistribution(LocalDate from, LocalDate to) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : repository.riskDistribution(from, to)) {
            counts.put(String.valueOf(row.get("risk_category")), longValue(row.get("total")));
        }
        List<DashboardReportResponse.RiskBucket> result = new ArrayList<>();
        result.add(new DashboardReportResponse.RiskBucket("Risiko rendah", "LOW",
                counts.getOrDefault("LOW", 0L)));
        result.add(new DashboardReportResponse.RiskBucket("Risiko sedang", "MEDIUM",
                counts.getOrDefault("MEDIUM", 0L)));
        result.add(new DashboardReportResponse.RiskBucket("Risiko tinggi", "HIGH",
                counts.getOrDefault("HIGH", 0L)));
        return result;
    }

    private static List<YearMonth> lastSixMonths() {
        YearMonth now = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int index = 5; index >= 0; index--) {
            months.add(now.minusMonths(index));
        }
        return months;
    }

    private static long longValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
