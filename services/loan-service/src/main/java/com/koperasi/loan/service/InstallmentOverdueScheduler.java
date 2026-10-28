package com.koperasi.loan.service;

import com.koperasi.loan.config.LoanProperties;
import com.koperasi.loan.domain.entity.LoanInstallment;
import com.koperasi.loan.domain.enums.InstallmentStatus;
import com.koperasi.loan.event.LoanEventPublisher;
import com.koperasi.loan.event.LoanEventType;
import com.koperasi.loan.repository.LoanInstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Daily job that marks unpaid installments past their due date as OVERDUE and escalates
 * loans that have been delinquent beyond the configured default threshold.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallmentOverdueScheduler {

    private final LoanInstallmentRepository installmentRepository;
    private final LoanService loanService;
    private final LoanEventPublisher loanEventPublisher;
    private final LoanProperties loanProperties;

    @Scheduled(cron = "${app.loan.overdue-cron:0 5 0 * * *}",
            zone = "${app.loan.scheduler-zone:Asia/Jakarta}")
    @Transactional
    public void markOverdueInstallments() {
        LocalDate today = LocalDate.now(ZoneId.of(loanProperties.getSchedulerZone()));
        List<LoanInstallment> candidates = installmentRepository
                .findByStatusAndDueDateBeforeOrderByDueDateAsc(
                        InstallmentStatus.PENDING, today,
                        PageRequest.of(0, Math.max(1, loanProperties.getOverdueBatchSize())));
        if (candidates.isEmpty()) {
            return;
        }

        Map<UUID, Integer> maxDaysOverdueByLoan = new LinkedHashMap<>();
        for (LoanInstallment installment : candidates) {
            int daysOverdue = (int) ChronoUnit.DAYS.between(installment.getDueDate(), today);
            installment.setStatus(InstallmentStatus.OVERDUE);
            installmentRepository.save(installment);

            UUID loanId = installment.getLoanApplication().getId();
            UUID borrowerId = installment.getLoanApplication().getBorrowerId();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("installmentId", installment.getId().toString());
            payload.put("loanApplicationId", loanId.toString());
            payload.put("borrowerId", borrowerId.toString());
            payload.put("installmentNumber", installment.getInstallmentNumber());
            payload.put("dueDate", installment.getDueDate().toString());
            payload.put("daysOverdue", daysOverdue);
            payload.put("amount", installment.getAmount());
            payload.put("detectedAt", Instant.now().toString());
            loanEventPublisher.publishAfterCommit(LoanEventType.INSTALLMENT_OVERDUE,
                    loanId.toString(), payload);

            maxDaysOverdueByLoan.merge(loanId, daysOverdue, Math::max);
        }

        int defaultThreshold = Math.max(1, loanProperties.getDefaultOverdueDays());
        maxDaysOverdueByLoan.forEach((loanId, daysOverdue) -> {
            if (daysOverdue >= defaultThreshold) {
                loanService.markDefaulted(loanId, daysOverdue);
            }
        });
        log.info("Scheduler overdue selesai: {} cicilan ditandai OVERDUE, {} pinjaman dievaluasi",
                candidates.size(), maxDaysOverdueByLoan.size());
    }
}
