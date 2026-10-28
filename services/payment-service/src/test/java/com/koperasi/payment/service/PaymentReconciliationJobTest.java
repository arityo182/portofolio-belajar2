package com.koperasi.payment.service;

import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.domain.enums.DisbursementStatus;
import com.koperasi.payment.domain.enums.PaymentGateway;
import com.koperasi.payment.domain.enums.PaymentStatus;
import com.koperasi.payment.domain.enums.SyncStatus;
import com.koperasi.payment.repository.LoanDisbursementRepository;
import com.koperasi.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationJobTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LoanDisbursementRepository loanDisbursementRepository;

    @Mock
    private LoanServiceClient loanServiceClient;

    @Test
    void reconcile_successfulCallback_marksPaymentAndDisbursementSynced() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("908333.33"), PaymentGateway.MIDTRANS, "idem-pay");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        LoanDisbursement disbursement = LoanDisbursement.create(UUID.randomUUID(),
                new BigDecimal("5000000.00"), PaymentGateway.MIDTRANS, "idem-disb");
        disbursement.setStatus(DisbursementStatus.COMPLETED);
        disbursement.setDisbursedAt(Instant.now());

        when(paymentRepository.findByStatusAndSyncStatusNotOrderByCreatedAtAsc(
                eq(PaymentStatus.SUCCESS), eq(SyncStatus.SYNCED), any()))
                .thenReturn(List.of(payment));
        when(loanDisbursementRepository.findByStatusAndSyncStatusNotOrderByDisbursedAtAsc(
                eq(DisbursementStatus.COMPLETED), eq(SyncStatus.SYNCED), any()))
                .thenReturn(List.of(disbursement));

        new PaymentReconciliationJob(paymentRepository, loanDisbursementRepository, loanServiceClient)
                .reconcile();

        assertThat(payment.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        assertThat(disbursement.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        verify(paymentRepository).save(payment);
        verify(loanDisbursementRepository).save(disbursement);
    }

    @Test
    void reconcile_failedCallback_keepsRowForNextRetry() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("908333.33"), PaymentGateway.MIDTRANS, "idem-pay-2");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());

        when(paymentRepository.findByStatusAndSyncStatusNotOrderByCreatedAtAsc(
                eq(PaymentStatus.SUCCESS), eq(SyncStatus.SYNCED), any()))
                .thenReturn(List.of(payment));
        when(loanDisbursementRepository.findByStatusAndSyncStatusNotOrderByDisbursedAtAsc(
                eq(DisbursementStatus.COMPLETED), eq(SyncStatus.SYNCED), any()))
                .thenReturn(List.of());
        doThrow(new RuntimeException("loan-service unavailable"))
                .when(loanServiceClient)
                .markInstallmentPaid(eq(payment.getLoanInstallmentId()), any());

        new PaymentReconciliationJob(paymentRepository, loanDisbursementRepository, loanServiceClient)
                .reconcile();

        assertThat(payment.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(payment.getSyncAttempts()).isEqualTo(1);
        assertThat(payment.getSyncError()).contains("loan-service unavailable");
        verify(paymentRepository).save(payment);
    }
}
