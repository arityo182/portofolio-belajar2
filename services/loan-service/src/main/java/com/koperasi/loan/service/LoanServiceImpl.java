package com.koperasi.loan.service;

import com.koperasi.loan.config.LoanProperties;
import com.koperasi.loan.domain.dto.CreateLoanApplicationRequest;
import com.koperasi.loan.domain.dto.CreditScoreResponse;
import com.koperasi.loan.domain.dto.LoanApplicationOwnerResponse;
import com.koperasi.loan.domain.dto.LoanApplicationResponse;
import com.koperasi.loan.domain.dto.LoanApplicationSummaryResponse;
import com.koperasi.loan.domain.dto.LoanInstallmentDetailsResponse;
import com.koperasi.loan.domain.dto.LoanInstallmentResponse;
import com.koperasi.loan.domain.dto.PageResponse;
import com.koperasi.loan.domain.dto.UpdateLoanStatusRequest;
import com.koperasi.loan.domain.entity.CreditScore;
import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.entity.LoanInstallment;
import com.koperasi.loan.domain.entity.LoanProduct;
import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.event.LoanEventPublisher;
import com.koperasi.loan.event.LoanEventType;
import com.koperasi.loan.event.ScoringCompletedEvent;
import com.koperasi.loan.exception.BusinessValidationException;
import com.koperasi.loan.exception.ConcurrentModificationException;
import com.koperasi.loan.exception.ForbiddenOperationException;
import com.koperasi.loan.exception.IdempotencyConflictException;
import com.koperasi.loan.exception.InstallmentNotFoundException;
import com.koperasi.loan.exception.InvalidStateTransitionException;
import com.koperasi.loan.exception.LoanNotFoundException;
import com.koperasi.loan.exception.RejectionReasonRequiredException;
import com.koperasi.loan.exception.ScoreNotFoundException;
import com.koperasi.loan.repository.CreditScoreRepository;
import com.koperasi.loan.repository.LoanApplicationRepository;
import com.koperasi.loan.repository.LoanInstallmentRepository;
import com.koperasi.loan.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private static final Set<String> PRIVILEGED_ROLES = Set.of("REVIEWER", "ADMIN");
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 100;

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanProductService loanProductService;
    private final CreditScoreRepository creditScoreRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final LoanStateMachine loanStateMachine;
    private final InstallmentService installmentService;
    private final DistributedLockService distributedLockService;
    private final LoanEventPublisher loanEventPublisher;
    private final LoanProperties loanProperties;

    @Override
    @Transactional
    public LoanApplicationResponse apply(CreateLoanApplicationRequest request, String idempotencyKey,
                                         JwtPrincipal principal) {
        validateIdempotencyKey(idempotencyKey);

        String lockKey = "lock:loan:submit:" + principal.userId() + ":" + idempotencyKey;
        String lockToken = distributedLockService.tryAcquire(lockKey,
                Duration.ofSeconds(loanProperties.getLockTtlSeconds()));
        if (lockToken == null) {
            log.warn("Gagal memperoleh distributed lock key={}", lockKey);
            throw new IdempotencyConflictException(
                    "Permintaan dengan Idempotency-Key yang sama sedang diproses");
        }
        boolean releaseAfterCommit = scheduleLockRelease(lockKey, lockToken);

        try {
            Optional<LoanApplication> existing = loanApplicationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                LoanApplication cached = existing.get();
                if (!cached.getBorrowerId().equals(principal.userId())) {
                    throw new IdempotencyConflictException("Idempotency-Key sudah digunakan oleh pengguna lain");
                }
                log.info("Mengembalikan pengajuan idempotent loanId={}", cached.getId());
                return LoanApplicationResponse.from(cached);
            }

            LoanProduct product = loanProductService.getActiveProduct(request.loanProductId());
            if (!product.supports(request.amount(), request.tenorMonths())) {
                throw new BusinessValidationException(
                        "Jumlah pinjaman atau tenor tidak sesuai dengan ketentuan produk " + product.getName());
            }

            LoanApplication application = LoanApplication.create(
                    principal.userId(),
                    product,
                    request.amount(),
                    request.tenorMonths(),
                    request.purpose().trim(),
                    idempotencyKey
            );
            try {
                application = loanApplicationRepository.saveAndFlush(application);
            } catch (DataIntegrityViolationException ex) {
                log.warn("Gagal menyimpan pengajuan idempotencyKey={}: {}", idempotencyKey,
                        ex.getMostSpecificCause().getMessage());
                throw new IdempotencyConflictException("Idempotency-Key sudah digunakan");
            }

            publishApplicationCreated(application, product);
            publishScoringRequested(application);
            log.info("Pengajuan pinjaman dibuat loanId={} borrowerId={}", application.getId(), principal.userId());
            return LoanApplicationResponse.from(application);
        } finally {
            if (!releaseAfterCommit) {
                releaseLock(lockKey, lockToken);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanApplicationSummaryResponse> list(JwtPrincipal principal, LoanStatus status,
                                                             int page, int size) {
        if (page < 0) {
            throw new BusinessValidationException("Parameter page tidak boleh negatif");
        }
        if (size < 1 || size > loanProperties.getMaxPageSize()) {
            throw new BusinessValidationException("Parameter size harus antara 1 dan " + loanProperties.getMaxPageSize());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LoanApplication> result;
        if (isPrivileged(principal)) {
            result = status == null
                    ? loanApplicationRepository.findAll(pageable)
                    : loanApplicationRepository.findByStatus(status, pageable);
        } else {
            result = status == null
                    ? loanApplicationRepository.findByBorrowerId(principal.userId(), pageable)
                    : loanApplicationRepository.findByBorrowerIdAndStatus(principal.userId(), status, pageable);
        }
        return PageResponse.from(result, LoanApplicationSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(UUID loanId, JwtPrincipal principal) {
        LoanApplication application = findApplication(loanId);
        ensureCanView(application, principal);
        return LoanApplicationResponse.from(application);
    }

    @Override
    @Transactional
    public LoanApplicationResponse updateStatus(UUID loanId, UpdateLoanStatusRequest request, JwtPrincipal reviewer) {
        LoanApplication application = findApplication(loanId);
        if (!Objects.equals(application.getVersion(), request.version())) {
            throw new ConcurrentModificationException(request.version(), application.getVersion());
        }
        if (request.status() != LoanStatus.APPROVED && request.status() != LoanStatus.REJECTED) {
            throw new BusinessValidationException("Status tujuan hanya boleh APPROVED atau REJECTED");
        }
        if (request.status() == LoanStatus.REJECTED
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new RejectionReasonRequiredException();
        }

        loanStateMachine.validateTransition(application.getStatus(), request.status());

        Instant now = Instant.now();
        application.setReviewerId(reviewer.userId());
        application.setReviewedAt(now);
        application.setStatus(request.status());
        application.setReviewNotes(normalizeNotes(request.notes()));
        if (request.status() == LoanStatus.REJECTED) {
            application.setRejectionReason(request.rejectionReason().trim());
        } else {
            application.setRejectionReason(null);
        }

        application = persistWithOptimisticLockCheck(application);

        if (request.status() == LoanStatus.APPROVED) {
            installmentService.generateSchedule(application, loanProperties.getInstallmentMethod());
            publishApproved(application, reviewer.userId(), Boolean.TRUE.equals(request.overrideScore()));
        } else {
            publishRejected(application, reviewer.userId());
        }
        log.info("Status pinjaman diubah loanId={} status={} reviewerId={}",
                application.getId(), application.getStatus(), reviewer.userId());
        return LoanApplicationResponse.from(application);
    }

    @Override
    @Transactional
    public LoanApplicationResponse disburse(UUID loanId) {
        return markDisbursed(loanId, Instant.now());
    }

    @Override
    @Transactional
    public LoanApplicationResponse markDisbursed(UUID loanId, Instant disbursedAt) {
        LoanApplication application = findApplication(loanId);
        if (application.getStatus() == LoanStatus.DISBURSED
                || application.getStatus() == LoanStatus.ACTIVE
                || application.getStatus() == LoanStatus.COMPLETED
                || application.getStatus() == LoanStatus.DEFAULT) {
            return LoanApplicationResponse.from(application);
        }

        loanStateMachine.validateTransition(application.getStatus(), LoanStatus.DISBURSED);
        LoanStatus previousStatus = application.getStatus();
        application.setStatus(LoanStatus.DISBURSED);
        application.setDisbursedAt(disbursedAt == null ? Instant.now() : disbursedAt);
        application = persistWithOptimisticLockCheck(application);
        if (!loanInstallmentRepository.existsByLoanApplicationId(application.getId())) {
            installmentService.generateSchedule(application, loanProperties.getInstallmentMethod());
        }
        publishStatusChanged(LoanEventType.APPLICATION_DISBURSED, application, Map.of(
                "loanApplicationId", application.getId().toString(),
                "borrowerId", application.getBorrowerId().toString(),
                "previousStatus", previousStatus.name(),
                "status", application.getStatus().name(),
                "disbursedAt", application.getDisbursedAt().toString(),
                "changedAt", Instant.now().toString()
        ));
        log.info("Pinjaman dicairkan loanId={} disbursedAt={}", application.getId(), application.getDisbursedAt());
        return LoanApplicationResponse.from(application);
    }

    @Override
    @Transactional
    public void markDefaulted(UUID loanId, int daysOverdue) {
        LoanApplication application = findApplication(loanId);
        if (application.getStatus() != LoanStatus.ACTIVE
                && application.getStatus() != LoanStatus.DISBURSED) {
            return;
        }
        LoanStatus previousStatus = application.getStatus();
        loanStateMachine.validateTransition(previousStatus, LoanStatus.DEFAULT);
        application.setStatus(LoanStatus.DEFAULT);
        application = persistWithOptimisticLockCheck(application);

        java.math.BigDecimal outstanding = loanInstallmentRepository
                .findByLoanApplicationIdAndStatusNot(loanId,
                        com.koperasi.loan.domain.enums.InstallmentStatus.PAID)
                .stream()
                .map(LoanInstallment::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loanApplicationId", application.getId().toString());
        payload.put("borrowerId", application.getBorrowerId().toString());
        payload.put("previousStatus", previousStatus.name());
        payload.put("status", application.getStatus().name());
        payload.put("totalOutstanding", outstanding);
        payload.put("daysOverdue", daysOverdue);
        payload.put("defaultedAt", Instant.now().toString());
        loanEventPublisher.publishAfterCommit(LoanEventType.LOAN_DEFAULTED,
                application.getId().toString(), payload);
        log.warn("Pinjaman masuk status DEFAULT loanId={} daysOverdue={}", loanId, daysOverdue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanInstallmentResponse> getInstallments(UUID loanId, JwtPrincipal principal) {
        LoanApplication application = findApplication(loanId);
        ensureCanView(application, principal);
        return loanInstallmentRepository.findByLoanApplicationIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(LoanInstallmentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CreditScoreResponse getScore(UUID loanId, JwtPrincipal principal) {
        LoanApplication application = findApplication(loanId);
        boolean isOwner = Objects.equals(application.getBorrowerId(), principal.userId());
        if (!isPrivileged(principal) && !isOwner) {
            throw new ForbiddenOperationException("Anda tidak memiliki akses ke skor pinjaman ini");
        }
        CreditScore creditScore = creditScoreRepository.findByLoanApplicationId(application.getId())
                .orElseThrow(() -> new ScoreNotFoundException(loanId));
        return CreditScoreResponse.from(creditScore);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationOwnerResponse getApplicationOwner(UUID loanApplicationId) {
        LoanApplication application = loanApplicationRepository.findById(loanApplicationId)
                .orElseThrow(() -> new LoanNotFoundException(loanApplicationId));
        return LoanApplicationOwnerResponse.from(application);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanInstallmentDetailsResponse getInstallmentDetails(UUID installmentId) {
        LoanInstallment installment = loanInstallmentRepository.findById(installmentId)
                .orElseThrow(() -> new InstallmentNotFoundException(installmentId));
        return LoanInstallmentDetailsResponse.from(installment);
    }

    @Override
    @Transactional
    public LoanInstallmentDetailsResponse markInstallmentPaid(UUID installmentId, Instant paidAt) {
        LoanInstallment installment = loanInstallmentRepository.findById(installmentId)
                .orElseThrow(() -> new InstallmentNotFoundException(installmentId));
        boolean newlyPaid = installment.getStatus()
                != com.koperasi.loan.domain.enums.InstallmentStatus.PAID;
        if (newlyPaid) {
            installment.setStatus(com.koperasi.loan.domain.enums.InstallmentStatus.PAID);
            installment.setPaidAt(paidAt == null ? Instant.now() : paidAt);
            installment = loanInstallmentRepository.save(installment);
            updateLoanLifecycleAfterPayment(installment.getLoanApplication().getId());
        }
        return LoanInstallmentDetailsResponse.from(installment);
    }

    private void updateLoanLifecycleAfterPayment(UUID loanApplicationId) {
        LoanApplication application = findApplication(loanApplicationId);
        long unpaid = loanInstallmentRepository.countByLoanApplicationIdAndStatusNot(
                loanApplicationId, com.koperasi.loan.domain.enums.InstallmentStatus.PAID);
        if (unpaid == 0) {
            if (application.getStatus() == LoanStatus.DISBURSED
                    || application.getStatus() == LoanStatus.ACTIVE
                    || application.getStatus() == LoanStatus.DEFAULT) {
                LoanStatus previousStatus = application.getStatus();
                loanStateMachine.validateTransition(previousStatus, LoanStatus.COMPLETED);
                application.setStatus(LoanStatus.COMPLETED);
                application.setCompletedAt(Instant.now());
                application = persistWithOptimisticLockCheck(application);
                publishStatusChanged(LoanEventType.APPLICATION_COMPLETED, application, Map.of(
                        "loanApplicationId", application.getId().toString(),
                        "borrowerId", application.getBorrowerId().toString(),
                        "previousStatus", previousStatus.name(),
                        "status", application.getStatus().name(),
                        "completedAt", application.getCompletedAt().toString(),
                        "changedAt", Instant.now().toString()
                ));
                log.info("Pinjaman lunas loanId={}", application.getId());
            }
            return;
        }

        if (application.getStatus() == LoanStatus.DISBURSED) {
            loanStateMachine.validateTransition(LoanStatus.DISBURSED, LoanStatus.ACTIVE);
            application.setStatus(LoanStatus.ACTIVE);
            application = persistWithOptimisticLockCheck(application);
            publishStatusChanged(LoanEventType.APPLICATION_ACTIVE, application, Map.of(
                    "loanApplicationId", application.getId().toString(),
                    "borrowerId", application.getBorrowerId().toString(),
                    "previousStatus", LoanStatus.DISBURSED.name(),
                    "status", application.getStatus().name(),
                    "changedAt", Instant.now().toString()
            ));
            log.info("Pinjaman aktif setelah pembayaran pertama loanId={}", application.getId());
        }
    }

    @Override
    @Transactional
    public void handleScoringCompleted(ScoringCompletedEvent event) {
        if (event.payload() == null || event.payload().loanApplicationId() == null) {
            log.warn("Event scoring.completed tidak lengkap, diabaikan");
            return;
        }
        ScoringCompletedEvent.ScoringPayload payload = event.payload();
        LoanApplication application = loanApplicationRepository.findWithProductById(payload.loanApplicationId())
                .orElse(null);
        if (application == null) {
            log.warn("Pengajuan tidak ditemukan untuk event scoring loanApplicationId={}",
                    payload.loanApplicationId());
            return;
        }
        if (payload.riskScore() == null || payload.riskCategory() == null
                || payload.approvalRecommendation() == null) {
            log.warn("Hasil scoring tidak lengkap untuk loanApplicationId={}", payload.loanApplicationId());
            return;
        }
        UUID scoreBorrowerId = payload.borrowerId() == null
                ? application.getBorrowerId()
                : payload.borrowerId();
        if (!scoreBorrowerId.equals(application.getBorrowerId())) {
            log.warn("Borrower pada event scoring tidak sesuai loanApplicationId={}", payload.loanApplicationId());
            return;
        }

        LoanApplication scoringApplication = application;
        CreditScore creditScore = creditScoreRepository.findByLoanApplicationId(scoringApplication.getId())
                .orElseGet(() -> CreditScore.create(scoringApplication, scoreBorrowerId));
        creditScore.applyResult(
                payload.riskScore(),
                payload.riskCategory(),
                payload.approvalRecommendation(),
                payload.features(),
                payload.modelVersion(),
                payload.processingTimeMs(),
                payload.scoredAt()
        );
        creditScoreRepository.save(creditScore);

        if (application.getStatus() == LoanStatus.PENDING || application.getStatus() == LoanStatus.SCORING) {
            LoanStatus previousStatus = application.getStatus();
            loanStateMachine.validateTransition(previousStatus, LoanStatus.UNDER_REVIEW);
            application.setStatus(LoanStatus.UNDER_REVIEW);
            application = persistWithOptimisticLockCheck(application);
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("loanApplicationId", application.getId().toString());
            payloadMap.put("borrowerId", application.getBorrowerId().toString());
            payloadMap.put("previousStatus", previousStatus.name());
            payloadMap.put("status", application.getStatus().name());
            payloadMap.put("changedAt", Instant.now().toString());
            loanEventPublisher.publishAfterCommit(LoanEventType.APPLICATION_UNDER_REVIEW,
                    application.getId().toString(), payloadMap);
        }
    }

    private LoanApplication findApplication(UUID loanId) {
        return loanApplicationRepository.findWithProductById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }

    private LoanApplication persistWithOptimisticLockCheck(LoanApplication application) {
        try {
            return loanApplicationRepository.saveAndFlush(application);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException(application.getVersion(), application.getVersion() + 1);
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void ensureCanView(LoanApplication application, JwtPrincipal principal) {
        if (!isPrivileged(principal) && !application.getBorrowerId().equals(principal.userId())) {
            throw new ForbiddenOperationException("Anda tidak memiliki akses ke pinjaman ini");
        }
    }

    private boolean isPrivileged(JwtPrincipal principal) {
        return principal.roles() != null && principal.roles().stream().anyMatch(PRIVILEGED_ROLES::contains);
    }

    private boolean scheduleLockRelease(String lockKey, String lockToken) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseLock(lockKey, lockToken);
            }
        });
        return true;
    }

    private void releaseLock(String lockKey, String lockToken) {
        try {
            distributedLockService.release(lockKey, lockToken);
        } catch (RuntimeException ex) {
            log.warn("Gagal melepaskan distributed lock key={}: {}", lockKey, ex.getMessage());
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessValidationException("Header Idempotency-Key wajib diisi");
        }
        if (idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BusinessValidationException("Idempotency-Key maksimal " + IDEMPOTENCY_KEY_MAX_LENGTH + " karakter");
        }
    }

    private void publishApplicationCreated(LoanApplication application, LoanProduct product) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loanApplicationId", application.getId().toString());
        payload.put("borrowerId", application.getBorrowerId().toString());
        payload.put("loanProductId", product.getId());
        payload.put("amount", application.getAmount());
        payload.put("tenorMonths", application.getTenorMonths());
        payload.put("purpose", application.getPurpose());
        payload.put("status", application.getStatus().name());
        payload.put("idempotencyKey", application.getIdempotencyKey());
        payload.put("submittedAt", application.getSubmittedAt().toString());
        loanEventPublisher.publishAfterCommit(LoanEventType.APPLICATION_CREATED,
                application.getId().toString(), payload);
    }

    private void publishScoringRequested(LoanApplication application) {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("amount", application.getAmount());
        features.put("tenorMonths", application.getTenorMonths());
        features.put("loanProductId", application.getLoanProduct().getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loanApplicationId", application.getId().toString());
        payload.put("borrowerId", application.getBorrowerId().toString());
        payload.put("features", features);
        payload.put("requestedAt", Instant.now().toString());
        loanEventPublisher.publishAfterCommit(LoanEventType.SCORING_REQUESTED,
                application.getId().toString(), payload);
    }

    private void publishApproved(LoanApplication application, UUID reviewerId, boolean overrideScore) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loanApplicationId", application.getId().toString());
        payload.put("borrowerId", application.getBorrowerId().toString());
        payload.put("amount", application.getAmount());
        payload.put("tenorMonths", application.getTenorMonths());
        payload.put("reviewerId", reviewerId.toString());
        payload.put("overrideScore", overrideScore);
        payload.put("approvedAt", application.getReviewedAt().toString());
        loanEventPublisher.publishAfterCommit(LoanEventType.APPLICATION_APPROVED,
                application.getId().toString(), payload);
    }

    private void publishRejected(LoanApplication application, UUID reviewerId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loanApplicationId", application.getId().toString());
        payload.put("borrowerId", application.getBorrowerId().toString());
        payload.put("reviewerId", reviewerId.toString());
        payload.put("rejectionReason", application.getRejectionReason());
        payload.put("rejectedAt", application.getReviewedAt().toString());
        loanEventPublisher.publishAfterCommit(LoanEventType.APPLICATION_REJECTED,
                application.getId().toString(), payload);
    }

    private void publishStatusChanged(String eventType, LoanApplication application, Map<String, Object> payload) {
        loanEventPublisher.publishAfterCommit(eventType, application.getId().toString(), payload);
    }
}
