package com.koperasi.payment.repository;

import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.enums.DisbursementStatus;
import com.koperasi.payment.domain.enums.SyncStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, UUID> {

    Optional<LoanDisbursement> findByLoanApplicationId(UUID loanApplicationId);

    Optional<LoanDisbursement> findByIdempotencyKey(String idempotencyKey);

    Optional<LoanDisbursement> findByExternalTransactionId(String externalTransactionId);

    boolean existsByLoanApplicationIdAndStatusIn(UUID loanApplicationId, Collection<DisbursementStatus> statuses);

    List<LoanDisbursement> findByStatusAndSyncStatusNotOrderByDisbursedAtAsc(
            DisbursementStatus status, SyncStatus syncStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from LoanDisbursement d where d.id = :id")
    Optional<LoanDisbursement> findByIdForUpdate(@Param("id") UUID id);
}
