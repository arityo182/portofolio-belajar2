package com.koperasi.loan.service;

import com.koperasi.loan.domain.enums.InstallmentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentCalculatorTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 24);

    private final InstallmentCalculator calculator = new InstallmentCalculator();

    @Test
    void calculateFlatRate_producesConstantInterestAndExactPrincipalRepayment() {
        BigDecimal principal = new BigDecimal("12000000.00");

        List<InstallmentSchedule> schedules = calculator.calculateFlatRate(
                principal, new BigDecimal("18.00"), 12, START_DATE);

        assertThat(schedules).hasSize(12);
        assertThat(schedules.get(0).installmentNumber()).isEqualTo(1);
        assertThat(schedules.get(0).principal()).isEqualByComparingTo("1000000.00");
        assertThat(schedules.get(0).interest()).isEqualByComparingTo("180000.00");
        assertThat(schedules.get(0).amount()).isEqualByComparingTo("1180000.00");
        assertThat(schedules.get(11).dueDate()).isEqualTo(START_DATE.plusMonths(12));
        assertThat(schedules.stream().map(InstallmentSchedule::principal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(principal);
        assertThat(schedules.stream().map(InstallmentSchedule::amount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("14160000.00");
    }

    @Test
    void calculateFlatRate_withRounding_adjustsLastInstallmentPrincipal() {
        BigDecimal principal = new BigDecimal("10000000.00");

        List<InstallmentSchedule> schedules = calculator.calculateFlatRate(
                principal, new BigDecimal("12.00"), 12, START_DATE);

        BigDecimal principalTotal = schedules.stream()
                .map(InstallmentSchedule::principal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(schedules).hasSize(12);
        assertThat(principalTotal).isEqualByComparingTo(principal);
        assertThat(schedules.get(11).principal()).isEqualByComparingTo("833333.37");
    }

    @Test
    void calculateAnnuity_producesLevelPaymentAndExactPrincipalRepayment() {
        BigDecimal principal = new BigDecimal("12000000.00");

        List<InstallmentSchedule> schedules = calculator.calculateAnnuity(
                principal, new BigDecimal("18.00"), 12, START_DATE);

        assertThat(schedules).hasSize(12);
        assertThat(schedules.get(0).interest()).isEqualByComparingTo("180000.00");
        assertThat(schedules.get(0).principal()).isPositive();
        assertThat(schedules.get(0).amount()).isEqualByComparingTo(schedules.get(1).amount());
        assertThat(schedules.stream().map(InstallmentSchedule::principal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(principal);
        assertThat(schedules.stream().map(InstallmentSchedule::amount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isGreaterThan(principal);
        assertThat(schedules.get(11).amount().subtract(schedules.get(0).amount()).abs())
                .isLessThanOrEqualTo(new BigDecimal("0.10"));
    }

    @Test
    void calculateAnnuity_withZeroRate_returnsEqualPrincipalWithoutInterest() {
        List<InstallmentSchedule> schedules = calculator.calculateAnnuity(
                new BigDecimal("1200000.00"), BigDecimal.ZERO, 12, START_DATE);

        assertThat(schedules).hasSize(12);
        assertThat(schedules).allSatisfy(installment -> {
            assertThat(installment.interest()).isEqualByComparingTo("0.00");
            assertThat(installment.principal()).isEqualByComparingTo("100000.00");
        });
    }

    @Test
    void calculate_withAnnuityMethod_delegatesToAnnuityCalculation() {
        List<InstallmentSchedule> schedules = calculator.calculate(
                new BigDecimal("5000000.00"), new BigDecimal("15.00"), 6, START_DATE, InstallmentMethod.ANNUITY);

        assertThat(schedules).hasSize(6);
        assertThat(schedules.get(0).dueDate()).isEqualTo(START_DATE.plusMonths(1));
    }

    @Test
    void calculate_withFlatMethod_delegatesToFlatCalculation() {
        List<InstallmentSchedule> schedules = calculator.calculate(
                new BigDecimal("5000000.00"), new BigDecimal("15.00"), 6, START_DATE, InstallmentMethod.FLAT);

        assertThat(schedules).hasSize(6);
        assertThat(schedules.get(0).interest()).isEqualByComparingTo("62500.00");
    }

    @Test
    void calculate_withInvalidInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ZERO, BigDecimal.TEN, 12, START_DATE, InstallmentMethod.FLAT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(new BigDecimal("1000"), new BigDecimal("-1"), 12, START_DATE, InstallmentMethod.FLAT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(new BigDecimal("1000"), BigDecimal.TEN, 0, START_DATE, InstallmentMethod.FLAT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(new BigDecimal("1000"), BigDecimal.TEN, 12, null, InstallmentMethod.FLAT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(null, BigDecimal.TEN, 12, START_DATE, InstallmentMethod.FLAT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
