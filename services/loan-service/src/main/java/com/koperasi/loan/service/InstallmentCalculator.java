package com.koperasi.loan.service;

import com.koperasi.loan.domain.enums.InstallmentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InstallmentCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext MATH_CONTEXT = new MathContext(20, ROUNDING);

    public List<InstallmentSchedule> calculate(BigDecimal principal, BigDecimal annualRatePercent,
                                               int months, LocalDate startDate, InstallmentMethod method) {
        validate(principal, annualRatePercent, months, startDate);
        return switch (method) {
            case FLAT -> calculateFlatRate(principal, annualRatePercent, months, startDate);
            case ANNUITY -> calculateAnnuity(principal, annualRatePercent, months, startDate);
        };
    }

    public List<InstallmentSchedule> calculateFlatRate(BigDecimal principal, BigDecimal annualRatePercent,
                                                       int months, LocalDate startDate) {
        validate(principal, annualRatePercent, months, startDate);
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(months), MONEY_SCALE, ROUNDING);
        BigDecimal monthlyInterest = principal
                .multiply(annualRatePercent)
                .divide(BigDecimal.valueOf(100), 10, ROUNDING)
                .divide(BigDecimal.valueOf(12), MONEY_SCALE, ROUNDING);

        List<InstallmentSchedule> schedules = new ArrayList<>(months);
        BigDecimal allocatedPrincipal = BigDecimal.ZERO;
        for (int number = 1; number <= months; number++) {
            BigDecimal principalPart = number == months
                    ? principal.subtract(allocatedPrincipal)
                    : monthlyPrincipal;
            allocatedPrincipal = allocatedPrincipal.add(principalPart);
            schedules.add(new InstallmentSchedule(
                    number,
                    startDate.plusMonths(number),
                    principalPart.add(monthlyInterest),
                    principalPart,
                    monthlyInterest
            ));
        }
        return schedules;
    }

    public List<InstallmentSchedule> calculateAnnuity(BigDecimal principal, BigDecimal annualRatePercent,
                                                      int months, LocalDate startDate) {
        validate(principal, annualRatePercent, months, startDate);

        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(100), 12, ROUNDING)
                .divide(BigDecimal.valueOf(12), 12, ROUNDING);

        if (monthlyRate.signum() == 0) {
            return calculateFlatRate(principal, BigDecimal.ZERO, months, startDate);
        }

        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months, MATH_CONTEXT);
        BigDecimal payment = principal
                .multiply(monthlyRate, MATH_CONTEXT)
                .multiply(factor, MATH_CONTEXT)
                .divide(factor.subtract(BigDecimal.ONE), MONEY_SCALE, ROUNDING);

        List<InstallmentSchedule> schedules = new ArrayList<>(months);
        BigDecimal balance = principal;
        for (int number = 1; number <= months; number++) {
            BigDecimal interest = balance.multiply(monthlyRate, MATH_CONTEXT).setScale(MONEY_SCALE, ROUNDING);
            BigDecimal principalPart = number == months ? balance : payment.subtract(interest);
            if (principalPart.compareTo(balance) > 0) {
                principalPart = balance;
            }
            principalPart = principalPart.setScale(MONEY_SCALE, ROUNDING);
            BigDecimal amount = principalPart.add(interest);
            schedules.add(new InstallmentSchedule(
                    number,
                    startDate.plusMonths(number),
                    amount,
                    principalPart,
                    interest
            ));
            balance = balance.subtract(principalPart);
        }
        return schedules;
    }

    private void validate(BigDecimal principal, BigDecimal annualRatePercent, int months, LocalDate startDate) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("Pokok pinjaman harus lebih besar dari nol");
        }
        if (annualRatePercent == null || annualRatePercent.signum() < 0) {
            throw new IllegalArgumentException("Suku bunga tidak boleh negatif");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Tenor harus lebih besar dari nol");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Tanggal mulai wajib diisi");
        }
    }
}
