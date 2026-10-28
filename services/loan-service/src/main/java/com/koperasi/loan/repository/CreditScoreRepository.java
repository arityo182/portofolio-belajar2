package com.koperasi.loan.repository;

import com.koperasi.loan.domain.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, UUID> {

    Optional<CreditScore> findByLoanApplicationId(UUID loanApplicationId);
}
