package com.koperasi.loan.repository;

import com.koperasi.loan.domain.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, UUID> {

    List<LoanInstallment> findByLoanApplicationIdOrderByInstallmentNumberAsc(UUID loanApplicationId);

    boolean existsByLoanApplicationId(UUID loanApplicationId);

    long countByLoanApplicationId(UUID loanApplicationId);

    long countByLoanApplicationIdAndStatusNot(UUID loanApplicationId,
                                              com.koperasi.loan.domain.enums.InstallmentStatus status);

    java.util.List<LoanInstallment> findByStatusAndDueDateBeforeOrderByDueDateAsc(
            com.koperasi.loan.domain.enums.InstallmentStatus status, java.time.LocalDate dueDate,
            org.springframework.data.domain.Pageable pageable);

    java.util.List<LoanInstallment> findByLoanApplicationIdAndStatusNot(
            UUID loanApplicationId, com.koperasi.loan.domain.enums.InstallmentStatus status);
}
