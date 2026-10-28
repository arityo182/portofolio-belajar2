package com.koperasi.loan.repository;

import com.koperasi.loan.domain.entity.LoanDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, UUID> {

    Optional<LoanDisbursement> findByLoanApplicationId(UUID loanApplicationId);

    boolean existsByLoanApplicationId(UUID loanApplicationId);
}
