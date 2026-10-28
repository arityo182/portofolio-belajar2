package com.koperasi.payment.repository;

import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.domain.enums.PaymentStatus;
import com.koperasi.payment.domain.enums.SyncStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByLoanInstallmentIdAndBorrowerId(UUID loanInstallmentId, UUID borrowerId);

    Optional<Payment> findFirstByLoanInstallmentIdOrderByCreatedAtDesc(UUID loanInstallmentId);

    Optional<Payment> findByExternalTransactionId(String externalTransactionId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByLoanInstallmentIdAndStatus(UUID loanInstallmentId, PaymentStatus status);

    List<Payment> findByStatusAndSyncStatusNotOrderByCreatedAtAsc(
            PaymentStatus status, SyncStatus syncStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
}
