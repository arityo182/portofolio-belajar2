package com.koperasi.payment.service;

import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.domain.enums.DisbursementStatus;
import com.koperasi.payment.domain.enums.PaymentStatus;
import com.koperasi.payment.domain.enums.SyncStatus;
import com.koperasi.payment.repository.LoanDisbursementRepository;
import com.koperasi.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Safety net for the payment -> loan-service best-effort callback. A successful gateway
 * callback is persisted first, then synchronized to loan-service. If loan-service was
 * unavailable, the row stays SUCCESS/COMPLETED with sync_status FAILED and this job keeps
 * retrying until the loan side acknowledges the idempotent update.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private static final int BATCH_SIZE = 100;

    private final PaymentRepository paymentRepository;
    private final LoanDisbursementRepository loanDisbursementRepository;
    private final LoanServiceClient loanServiceClient;

    @Scheduled(fixedDelayString = "${app.payment.reconciliation-interval-ms:300000}",
            initialDelayString = "${app.payment.reconciliation-initial-delay-ms:60000}")
    public void reconcile() {
        List<Payment> payments = paymentRepository
                .findByStatusAndSyncStatusNotOrderByCreatedAtAsc(
                        PaymentStatus.SUCCESS, SyncStatus.SYNCED,
                        PageRequest.of(0, BATCH_SIZE));
        int paymentFailures = 0;
        for (Payment payment : payments) {
            try {
                loanServiceClient.markInstallmentPaid(
                        payment.getLoanInstallmentId(), payment.getPaidAt());
                payment.markSynced();
                paymentRepository.save(payment);
            } catch (RuntimeException ex) {
                paymentFailures++;
                payment.markSyncFailed(ex.getMessage());
                paymentRepository.save(payment);
                log.warn("Rekonsiliasi pembayaran gagal paymentId={} attempt={}: {}",
                        payment.getId(), payment.getSyncAttempts(), ex.getMessage());
            }
        }

        List<LoanDisbursement> disbursements = loanDisbursementRepository
                .findByStatusAndSyncStatusNotOrderByDisbursedAtAsc(
                        DisbursementStatus.COMPLETED, SyncStatus.SYNCED,
                        PageRequest.of(0, BATCH_SIZE));
        int disbursementFailures = 0;
        for (LoanDisbursement disbursement : disbursements) {
            try {
                loanServiceClient.markDisbursed(
                        disbursement.getLoanApplicationId(), disbursement.getDisbursedAt());
                disbursement.markSynced();
                loanDisbursementRepository.save(disbursement);
            } catch (RuntimeException ex) {
                disbursementFailures++;
                disbursement.markSyncFailed(ex.getMessage());
                loanDisbursementRepository.save(disbursement);
                log.warn("Rekonsiliasi pencairan gagal disbursementId={} attempt={}: {}",
                        disbursement.getId(), disbursement.getSyncAttempts(), ex.getMessage());
            }
        }

        if (!payments.isEmpty() || !disbursements.isEmpty()) {
            log.info("Rekonsiliasi selesai: payments={} gagal={} disbursements={} gagal={}",
                    payments.size(), paymentFailures,
                    disbursements.size(), disbursementFailures);
        }
    }
}
