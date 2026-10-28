package com.koperasi.loan.service;

import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.entity.LoanInstallment;
import com.koperasi.loan.domain.enums.InstallmentMethod;
import com.koperasi.loan.repository.LoanInstallmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstallmentService {

    private final LoanInstallmentRepository installmentRepository;
    private final InstallmentCalculator installmentCalculator;

    @Transactional
    public List<LoanInstallment> generateSchedule(LoanApplication application, InstallmentMethod method) {
        if (installmentRepository.existsByLoanApplicationId(application.getId())) {
            return installmentRepository.findByLoanApplicationIdOrderByInstallmentNumberAsc(application.getId());
        }

        List<InstallmentSchedule> schedules = installmentCalculator.calculate(
                application.getAmount(),
                application.getLoanProduct().getInterestRateAnnual(),
                application.getTenorMonths(),
                LocalDate.now(ZoneOffset.UTC),
                method
        );

        List<LoanInstallment> installments = schedules.stream()
                .map(schedule -> LoanInstallment.create(
                        application,
                        schedule.installmentNumber(),
                        schedule.dueDate(),
                        schedule.amount(),
                        schedule.principal(),
                        schedule.interest()
                ))
                .toList();
        return installmentRepository.saveAll(installments);
    }

    public List<InstallmentSchedule> calculateSchedule(LoanApplication application, InstallmentMethod method) {
        return installmentCalculator.calculate(
                application.getAmount(),
                application.getLoanProduct().getInterestRateAnnual(),
                application.getTenorMonths(),
                LocalDate.now(ZoneOffset.UTC),
                method
        );
    }
}
