package com.koperasi.reporting.controller;

import com.koperasi.reporting.domain.dto.ApiResponse;
import com.koperasi.reporting.domain.dto.DashboardReportResponse;
import com.koperasi.reporting.domain.dto.LoanReportRow;
import com.koperasi.reporting.domain.dto.PageResponse;
import com.koperasi.reporting.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public ResponseEntity<ApiResponse<DashboardReportResponse>> dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.dashboard(from, to), "OK"));
    }

    @GetMapping("/loans")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public ResponseEntity<ApiResponse<PageResponse<LoanReportRow>>> loans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                reportService.loans(from, to, status, page, size), "OK"));
    }

    @GetMapping("/loans/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportLoans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) throws IOException {
        if ("csv".equalsIgnoreCase(format)) {
            String csv = reportService.exportCsv(from, to, status);
            String filename = "loans-report-" + LocalDate.now() + ".csv";
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/csv");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
            return;
        }
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = reportService.exportPdf(from, to, status);
            String filename = "loans-report-" + LocalDate.now() + ".pdf";
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            response.getOutputStream().write(pdf);
            return;
        }
        response.setStatus(422);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"code\":\"VALIDATION_ERROR\","
                        + "\"message\":\"Format export yang didukung adalah csv atau pdf\"}");
    }
}
