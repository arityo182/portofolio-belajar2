package com.koperasi.loan.repository;

import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    @EntityGraph(attributePaths = {"loanProduct"})
    Optional<LoanApplication> findWithProductById(UUID id);

    @EntityGraph(attributePaths = {"loanProduct"})
    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"loanProduct"})
    Page<LoanApplication> findByBorrowerId(UUID borrowerId, Pageable pageable);

    @EntityGraph(attributePaths = {"loanProduct"})
    Page<LoanApplication> findByBorrowerIdAndStatus(UUID borrowerId, LoanStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"loanProduct"})
    Page<LoanApplication> findByStatus(LoanStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"loanProduct"})
    Page<LoanApplication> findAll(Pageable pageable);
}
