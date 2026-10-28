package com.koperasi.loan.service;

import com.koperasi.loan.config.LoanProperties;
import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.entity.LoanInstallment;
import com.koperasi.loan.domain.enums.InstallmentStatus;
import com.koperasi.loan.event.LoanEventPublisher;
import com.koperasi.loan.event.LoanEventType;
import com.koperasi.loan.repository.LoanInstallmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallmentOverdueSchedulerTest {

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private LoanService loanService;

    @Mock
    private LoanEventPublisher loanEventPublisher;

    @Test
    void markOverdueInstallments_marksOverdueAndEscalatesDefault() {
        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setBorrowerId(UUID.randomUUID());

        LoanInstallment installment = LoanInstallment.create(
                application, 1, LocalDate.now().minusDays(100),
                new BigDecimal("908333.33"), new BigDecimal("833333.33"),
                new BigDecimal("75000.00"));

        when(installmentRepository.findByStatusAndDueDateBeforeOrderByDueDateAsc(
                eq(InstallmentStatus.PENDING), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of(installment));
        when(installmentRepository.save(installment)).thenReturn(installment);

        LoanProperties properties = new LoanProperties();
        properties.setDefaultOverdueDays(90);
        properties.setOverdueBatchSize(500);
        properties.setSchedulerZone("Asia/Jakarta");
        InstallmentOverdueScheduler scheduler = new InstallmentOverdueScheduler(
                installmentRepository, loanService, loanEventPublisher, properties);

        scheduler.markOverdueInstallments();

        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.INSTALLMENT_OVERDUE),
                eq(application.getId().toString()),
                anyMap());
        verify(loanService).markDefaulted(eq(application.getId()), eq(100));
    }
}
