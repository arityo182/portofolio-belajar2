package com.koperasi.loan.service;

import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.entity.LoanInstallment;
import com.koperasi.loan.domain.entity.LoanProduct;
import com.koperasi.loan.domain.enums.InstallmentMethod;
import com.koperasi.loan.repository.LoanInstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallmentServiceTest {

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private InstallmentCalculator installmentCalculator;

    private InstallmentService installmentService;
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        installmentService = new InstallmentService(loanInstallmentRepository, installmentCalculator);
        application = LoanApplication.create(
                UUID.randomUUID(), product(), new BigDecimal("12000000.00"), 12,
                "Modal usaha", "key-" + UUID.randomUUID());
    }

    @Test
    void generateSchedule_whenNoScheduleExists_calculatesAndPersistsInstallments() {
        InstallmentSchedule schedule = new InstallmentSchedule(
                1, LocalDate.now().plusMonths(1), new BigDecimal("1180000.00"),
                new BigDecimal("1000000.00"), new BigDecimal("180000.00"));
        when(loanInstallmentRepository.existsByLoanApplicationId(application.getId())).thenReturn(false);
        when(installmentCalculator.calculate(
                eq(application.getAmount()),
                eq(application.getLoanProduct().getInterestRateAnnual()),
                eq(application.getTenorMonths()),
                any(LocalDate.class),
                eq(InstallmentMethod.FLAT)))
                .thenReturn(List.of(schedule));
        when(loanInstallmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LoanInstallment> installments = installmentService.generateSchedule(application, InstallmentMethod.FLAT);

        assertThat(installments).hasSize(1);
        assertThat(installments.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(installments.get(0).getDueDate()).isEqualTo(schedule.dueDate());
    }

    @Test
    void generateSchedule_whenScheduleAlreadyExists_returnsExistingWithoutRecalculation() {
        LoanInstallment existing = LoanInstallment.create(
                application, 1, LocalDate.now().plusMonths(1),
                new BigDecimal("1180000.00"), new BigDecimal("1000000.00"), new BigDecimal("180000.00"));
        when(loanInstallmentRepository.existsByLoanApplicationId(application.getId())).thenReturn(true);
        when(loanInstallmentRepository.findByLoanApplicationIdOrderByInstallmentNumberAsc(application.getId()))
                .thenReturn(List.of(existing));

        List<LoanInstallment> installments = installmentService.generateSchedule(application, InstallmentMethod.FLAT);

        assertThat(installments).containsExactly(existing);
        verifyNoInteractions(installmentCalculator);
    }

    @Test
    void calculateSchedule_delegatesToCalculator() {
        when(installmentCalculator.calculate(
                eq(application.getAmount()),
                eq(application.getLoanProduct().getInterestRateAnnual()),
                eq(application.getTenorMonths()),
                any(LocalDate.class),
                eq(InstallmentMethod.ANNUITY)))
                .thenReturn(List.of());

        List<InstallmentSchedule> schedules = installmentService.calculateSchedule(application, InstallmentMethod.ANNUITY);

        assertThat(schedules).isEmpty();
        verify(installmentCalculator).calculate(
                eq(application.getAmount()),
                eq(application.getLoanProduct().getInterestRateAnnual()),
                eq(application.getTenorMonths()),
                any(LocalDate.class),
                eq(InstallmentMethod.ANNUITY));
    }

    private LoanProduct product() {
        LoanProduct product = new LoanProduct();
        product.setId(1);
        product.setName("Modal Usaha Mikro");
        product.setMinAmount(new BigDecimal("1000000.00"));
        product.setMaxAmount(new BigDecimal("50000000.00"));
        product.setInterestRateAnnual(new BigDecimal("18.00"));
        product.setMinTenorMonths(3);
        product.setMaxTenorMonths(24);
        product.setActive(true);
        return product;
    }
}
