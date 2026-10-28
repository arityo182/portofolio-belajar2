package com.koperasi.loan.service;

import com.koperasi.loan.config.LoanProperties;
import com.koperasi.loan.domain.dto.CreateLoanApplicationRequest;
import com.koperasi.loan.domain.dto.CreditScoreResponse;
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
import com.koperasi.loan.domain.enums.ApprovalRecommendation;
import com.koperasi.loan.domain.enums.InstallmentMethod;
import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.domain.enums.RiskCategory;
import com.koperasi.loan.event.LoanEventPublisher;
import com.koperasi.loan.event.LoanEventType;
import com.koperasi.loan.event.ScoringCompletedEvent;
import com.koperasi.loan.exception.BusinessValidationException;
import com.koperasi.loan.exception.ConcurrentModificationException;
import com.koperasi.loan.exception.ForbiddenOperationException;
import com.koperasi.loan.exception.IdempotencyConflictException;
import com.koperasi.loan.exception.InvalidStateTransitionException;
import com.koperasi.loan.exception.LoanNotFoundException;
import com.koperasi.loan.exception.LoanProductNotFoundException;
import com.koperasi.loan.exception.RejectionReasonRequiredException;
import com.koperasi.loan.exception.ScoreNotFoundException;
import com.koperasi.loan.repository.CreditScoreRepository;
import com.koperasi.loan.repository.LoanApplicationRepository;
import com.koperasi.loan.repository.LoanInstallmentRepository;
import com.koperasi.loan.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final BigDecimal AMOUNT = new BigDecimal("10000000.00");
    private static final int TENOR = 12;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanProductService loanProductService;

    @Mock
    private CreditScoreRepository creditScoreRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private InstallmentService installmentService;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private LoanEventPublisher loanEventPublisher;

    private LoanServiceImpl loanService;
    private LoanProperties loanProperties;
    private LoanProduct product;
    private JwtPrincipal borrower;
    private JwtPrincipal otherBorrower;
    private JwtPrincipal reviewer;

    @BeforeEach
    void setUp() {
        loanProperties = new LoanProperties();
        loanProperties.setLockTtlSeconds(10);
        loanProperties.setMaxPageSize(100);
        loanProperties.setInstallmentMethod(InstallmentMethod.FLAT);
        loanService = new LoanServiceImpl(
                loanApplicationRepository,
                loanProductService,
                creditScoreRepository,
                loanInstallmentRepository,
                new LoanStateMachine(),
                installmentService,
                distributedLockService,
                loanEventPublisher,
                loanProperties
        );
        product = product();
        borrower = principal(UUID.randomUUID(), "BORROWER");
        otherBorrower = principal(UUID.randomUUID(), "BORROWER");
        reviewer = principal(UUID.randomUUID(), "REVIEWER");
    }

    @Test
    void apply_withValidRequest_createsPendingApplicationAndPublishesEvents() {
        when(loanProductService.getActiveProduct(product.getId())).thenReturn(product);
        when(distributedLockService.tryAcquire(anyString(), eq(Duration.ofSeconds(10)))).thenReturn("lock-token");
        when(loanApplicationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(loanApplicationRepository.saveAndFlush(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplicationResponse response = loanService.apply(
                new CreateLoanApplicationRequest(product.getId(), AMOUNT, TENOR, "Modal usaha"),
                "idem-1",
                borrower
        );

        assertThat(response.loanId()).isNotNull();
        assertThat(response.borrowerId()).isEqualTo(borrower.userId());
        assertThat(response.status()).isEqualTo(LoanStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(response.tenorMonths()).isEqualTo(TENOR);

        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.APPLICATION_CREATED),
                eq(response.loanId().toString()),
                argThat(payload -> "PENDING".equals(payload.get("status"))
                        && "idem-1".equals(payload.get("idempotencyKey")))
        );
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.SCORING_REQUESTED),
                eq(response.loanId().toString()),
                argThat(payload -> payload.get("features") instanceof Map<?, ?>)
        );
        verify(distributedLockService).release(
                argThat(key -> key.contains(borrower.userId().toString()) && key.endsWith(":idem-1")),
                eq("lock-token")
        );
    }

    @Test
    void apply_withExistingIdempotencyKey_returnsCachedApplicationWithoutPublishingEvents() {
        LoanApplication existing = application(LoanStatus.PENDING, 0L);
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock-token");
        when(loanApplicationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        LoanApplicationResponse response = loanService.apply(
                request(), "idem-1", borrower);

        assertThat(response.loanId()).isEqualTo(existing.getId());
        verify(loanApplicationRepository, never()).saveAndFlush(any(LoanApplication.class));
        verifyNoInteractions(loanEventPublisher);
        verify(distributedLockService).release(anyString(), eq("lock-token"));
    }

    @Test
    void apply_withIdempotencyKeyOwnedByAnotherBorrower_throwsConflict() {
        LoanApplication existing = application(LoanStatus.PENDING, 0L);
        existing.setBorrowerId(otherBorrower.userId());
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock-token");
        when(loanApplicationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> loanService.apply(request(), "idem-1", borrower))
                .isInstanceOf(IdempotencyConflictException.class);

        verify(distributedLockService).release(anyString(), eq("lock-token"));
    }

    @Test
    void apply_whenLockCannotBeAcquired_throwsIdempotencyConflict() {
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn(null);

        assertThatThrownBy(() -> loanService.apply(request(), "idem-1", borrower))
                .isInstanceOf(IdempotencyConflictException.class);

        verifyNoInteractions(loanApplicationRepository, loanEventPublisher);
        verify(distributedLockService, never()).release(anyString(), anyString());
    }

    @Test
    void apply_whenProductDoesNotSupportAmount_throwsBusinessValidation() {
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock-token");
        when(loanApplicationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(loanProductService.getActiveProduct(product.getId())).thenReturn(product);

        assertThatThrownBy(() -> loanService.apply(
                new CreateLoanApplicationRequest(product.getId(), new BigDecimal("999999999.00"), TENOR, "Modal"),
                "idem-1", borrower))
                .isInstanceOf(BusinessValidationException.class);

        verify(distributedLockService).release(anyString(), eq("lock-token"));
    }

    @Test
    void apply_whenProductIsMissing_throwsLoanProductNotFound() {
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock-token");
        when(loanApplicationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(loanProductService.getActiveProduct(99)).thenThrow(new LoanProductNotFoundException(99));

        assertThatThrownBy(() -> loanService.apply(
                new CreateLoanApplicationRequest(99, AMOUNT, TENOR, "Modal"), "idem-1", borrower))
                .isInstanceOf(LoanProductNotFoundException.class);
    }

    @Test
    void apply_withBlankOrTooLongIdempotencyKey_throwsBusinessValidation() {
        assertThatThrownBy(() -> loanService.apply(request(), " ", borrower))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> loanService.apply(request(), "x".repeat(101), borrower))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loanProductService);
    }

    @Test
    void updateStatus_approve_generatesInstallmentsAndPublishesApprovedEvent() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 3L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(loanApplicationRepository.saveAndFlush(application)).thenReturn(application);
        when(installmentService.generateSchedule(application, InstallmentMethod.FLAT)).thenReturn(List.of());

        LoanApplicationResponse response = loanService.updateStatus(
                application.getId(),
                new UpdateLoanStatusRequest(LoanStatus.APPROVED, null, true, null, 3L),
                reviewer
        );

        assertThat(response.status()).isEqualTo(LoanStatus.APPROVED);
        assertThat(response.reviewerId()).isEqualTo(reviewer.userId());
        assertThat(response.reviewedAt()).isNotNull();
        verify(installmentService).generateSchedule(application, InstallmentMethod.FLAT);
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.APPLICATION_APPROVED),
                eq(application.getId().toString()),
                argThat(payload -> Boolean.TRUE.equals(payload.get("overrideScore")))
        );
    }

    @Test
    void updateStatus_withStaleVersion_throwsConcurrentModification() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 4L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanService.updateStatus(
                application.getId(),
                new UpdateLoanStatusRequest(LoanStatus.APPROVED, null, false, null, 3L),
                reviewer))
                .isInstanceOf(ConcurrentModificationException.class);

        verify(loanApplicationRepository, never()).saveAndFlush(any(LoanApplication.class));
        verifyNoInteractions(loanEventPublisher, installmentService);
    }

    @Test
    void updateStatus_rejectWithoutReason_throwsRejectionReasonRequired() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 3L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanService.updateStatus(
                application.getId(),
                new UpdateLoanStatusRequest(LoanStatus.REJECTED, "  ", false, null, 3L),
                reviewer))
                .isInstanceOf(RejectionReasonRequiredException.class);

        verify(loanApplicationRepository, never()).saveAndFlush(any(LoanApplication.class));
    }

    @Test
    void updateStatus_rejectWithReason_setsReasonAndPublishesRejectedEvent() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 3L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(loanApplicationRepository.saveAndFlush(application)).thenReturn(application);

        LoanApplicationResponse response = loanService.updateStatus(
                application.getId(),
                new UpdateLoanStatusRequest(LoanStatus.REJECTED, "  Risiko tinggi  ", false, "  Catatan reviewer  ", 3L),
                reviewer
        );

        assertThat(response.status()).isEqualTo(LoanStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Risiko tinggi");
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.APPLICATION_REJECTED),
                eq(application.getId().toString()),
                argThat(payload -> "Risiko tinggi".equals(payload.get("rejectionReason")))
        );
        verifyNoInteractions(installmentService);
    }

    @Test
    void updateStatus_withInvalidTargetStatus_throwsBusinessValidation() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 3L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanService.updateStatus(
                application.getId(),
                new UpdateLoanStatusRequest(LoanStatus.DISBURSED, null, false, null, 3L),
                reviewer))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void updateStatus_withInvalidStateTransition_throwsInvalidStateTransition() {
        LoanApplication application = application(LoanStatus.COMPLETED, 3L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanService.updateStatus(
                application.getId(),
                new UpdateLoanStatusRequest(LoanStatus.APPROVED, null, false, null, 3L),
                reviewer))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void updateStatus_whenOptimisticLockFails_throwsConcurrentModification() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 3L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(loanApplicationRepository.saveAndFlush(application))
                .thenThrow(new ObjectOptimisticLockingFailureException(LoanApplication.class, application.getId()));

        assertThatThrownBy(() -> loanService.updateStatus(
                application.getId(),
                new UpdateLoanStatusRequest(LoanStatus.APPROVED, null, false, null, 3L),
                reviewer))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void getById_asOwner_returnsApplication() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        LoanApplicationResponse response = loanService.getById(application.getId(), borrower);

        assertThat(response.loanId()).isEqualTo(application.getId());
    }

    @Test
    void getById_asOtherBorrower_throwsForbidden() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanService.getById(application.getId(), otherBorrower))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void getById_asReviewer_returnsAnyApplication() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        assertThat(loanService.getById(application.getId(), reviewer).loanId()).isEqualTo(application.getId());
    }

    @Test
    void getById_whenMissing_throwsLoanNotFound() {
        UUID id = UUID.randomUUID();
        when(loanApplicationRepository.findWithProductById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getById(id, borrower))
                .isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    void list_asBorrower_returnsOwnApplications() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findByBorrowerId(eq(borrower.userId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application)));

        PageResponse<LoanApplicationSummaryResponse> response = loanService.list(borrower, null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).loanId()).isEqualTo(application.getId());
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void list_asReviewer_returnsAllApplications() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application)));

        PageResponse<LoanApplicationSummaryResponse> response = loanService.list(reviewer, null, 0, 20);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void list_withStatusFilter_usesFilteredQuery() {
        LoanApplication application = application(LoanStatus.APPROVED, 1L);
        when(loanApplicationRepository.findByBorrowerIdAndStatus(
                eq(borrower.userId()), eq(LoanStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application)));

        PageResponse<LoanApplicationSummaryResponse> response =
                loanService.list(borrower, LoanStatus.APPROVED, 0, 10);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void list_withInvalidPagination_throwsBusinessValidation() {
        assertThatThrownBy(() -> loanService.list(borrower, null, -1, 20))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> loanService.list(borrower, null, 0, 0))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> loanService.list(borrower, null, 0, 101))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void getInstallments_mapsInstallmentEntities() {
        LoanApplication application = application(LoanStatus.APPROVED, 1L);
        LoanInstallment installment = LoanInstallment.create(
                application, 1, LocalDate.now().plusMonths(1),
                new BigDecimal("1180000.00"), new BigDecimal("1000000.00"), new BigDecimal("180000.00"));
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(loanInstallmentRepository.findByLoanApplicationIdOrderByInstallmentNumberAsc(application.getId()))
                .thenReturn(List.of(installment));

        List<LoanInstallmentResponse> response = loanService.getInstallments(application.getId(), borrower);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).installmentNumber()).isEqualTo(1);
        assertThat(response.get(0).amount()).isEqualByComparingTo("1180000.00");
    }

    @Test
    void getScore_asReviewer_returnsScore() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 1L);
        CreditScore score = score(application);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(creditScoreRepository.findByLoanApplicationId(application.getId())).thenReturn(Optional.of(score));

        CreditScoreResponse response = loanService.getScore(application.getId(), reviewer);

        assertThat(response.loanApplicationId()).isEqualTo(application.getId());
        assertThat(response.score()).isEqualByComparingTo("78.50");
        assertThat(response.features()).containsKey("payment_consistency_score");
    }

    @Test
    void getScore_asOwnerBorrower_returnsScore() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 1L);
        CreditScore score = score(application);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(creditScoreRepository.findByLoanApplicationId(application.getId())).thenReturn(Optional.of(score));

        CreditScoreResponse response = loanService.getScore(application.getId(), borrower);

        assertThat(response.loanApplicationId()).isEqualTo(application.getId());
        assertThat(response.score()).isEqualByComparingTo("78.50");
        assertThat(response.riskCategory()).isEqualTo(RiskCategory.MEDIUM);
    }

    @Test
    void getScore_asOtherBorrower_throwsForbidden() {
        LoanApplication application = application(LoanStatus.UNDER_REVIEW, 1L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanService.getScore(application.getId(), otherBorrower))
                .isInstanceOf(ForbiddenOperationException.class);

        verifyNoInteractions(creditScoreRepository);
    }

    @Test
    void getScore_whenMissing_throwsScoreNotFound() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(creditScoreRepository.findByLoanApplicationId(application.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getScore(application.getId(), reviewer))
                .isInstanceOf(ScoreNotFoundException.class);
    }

    @Test
    void handleScoringCompleted_transitionsPendingApplicationToUnderReviewAndSavesScore() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(creditScoreRepository.findByLoanApplicationId(application.getId())).thenReturn(Optional.empty());
        when(creditScoreRepository.save(any(CreditScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanApplicationRepository.saveAndFlush(application)).thenReturn(application);

        loanService.handleScoringCompleted(scoringEvent(application));

        assertThat(application.getStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
        verify(creditScoreRepository).save(any(CreditScore.class));
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.APPLICATION_UNDER_REVIEW),
                eq(application.getId().toString()),
                argThat(payload -> "PENDING".equals(payload.get("previousStatus"))
                        && "UNDER_REVIEW".equals(payload.get("status")))
        );
    }

    @Test
    void handleScoringCompleted_whenApplicationMissing_doesNothing() {
        UUID applicationId = UUID.randomUUID();
        when(loanApplicationRepository.findWithProductById(applicationId)).thenReturn(Optional.empty());
        ScoringCompletedEvent event = scoringEventFor(applicationId);

        assertThatCode(() -> loanService.handleScoringCompleted(event)).doesNotThrowAnyException();

        verifyNoInteractions(creditScoreRepository, loanEventPublisher);
    }

    @Test
    void handleScoringCompleted_withIncompletePayload_doesNothing() {
        LoanApplication application = application(LoanStatus.PENDING, 0L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        ScoringCompletedEvent event = new ScoringCompletedEvent(
                "event", "loan.scoring.completed", Instant.now().toString(), "trace", "1.0",
                new ScoringCompletedEvent.ScoringPayload(
                        application.getId(), borrower.userId(), null, null, null,
                        BigDecimal.ONE, Map.of(), "v2.3.1", 100, "SUCCESS", Instant.now())
        );

        assertThatCode(() -> loanService.handleScoringCompleted(event)).doesNotThrowAnyException();

        verifyNoInteractions(creditScoreRepository, loanEventPublisher);
    }

    @Test
    void disburse_approvedApplication_generatesInstallmentsAndPublishesEvent() {
        LoanApplication application = application(LoanStatus.APPROVED, 1L);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(loanApplicationRepository.saveAndFlush(application)).thenReturn(application);
        when(loanInstallmentRepository.existsByLoanApplicationId(application.getId())).thenReturn(false);
        when(installmentService.generateSchedule(application, InstallmentMethod.FLAT)).thenReturn(List.of());

        LoanApplicationResponse response = loanService.disburse(application.getId());

        assertThat(response.status()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(response.disbursedAt()).isNotNull();
        verify(installmentService).generateSchedule(application, InstallmentMethod.FLAT);
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.APPLICATION_DISBURSED),
                eq(application.getId().toString()),
                anyMap()
        );
    }

    @Test
    void markInstallmentPaid_firstPayment_activatesDisbursedLoan() {
        LoanApplication application = application(LoanStatus.DISBURSED, 3L);
        LoanInstallment installment = LoanInstallment.create(
                application, 1, LocalDate.now().plusMonths(1),
                new BigDecimal("908333.33"), new BigDecimal("833333.33"), new BigDecimal("75000.00"));
        when(loanInstallmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));
        when(loanInstallmentRepository.save(installment)).thenReturn(installment);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(loanInstallmentRepository.countByLoanApplicationIdAndStatusNot(
                application.getId(), com.koperasi.loan.domain.enums.InstallmentStatus.PAID)).thenReturn(5L);
        when(loanApplicationRepository.saveAndFlush(application)).thenReturn(application);

        loanService.markInstallmentPaid(installment.getId(), Instant.now());

        assertThat(application.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.APPLICATION_ACTIVE),
                eq(application.getId().toString()),
                anyMap());
    }

    @Test
    void markInstallmentPaid_lastPayment_completesLoan() {
        LoanApplication application = application(LoanStatus.ACTIVE, 4L);
        LoanInstallment installment = LoanInstallment.create(
                application, 12, LocalDate.now().plusMonths(1),
                new BigDecimal("908333.33"), new BigDecimal("833333.33"), new BigDecimal("75000.00"));
        when(loanInstallmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));
        when(loanInstallmentRepository.save(installment)).thenReturn(installment);
        when(loanApplicationRepository.findWithProductById(application.getId())).thenReturn(Optional.of(application));
        when(loanInstallmentRepository.countByLoanApplicationIdAndStatusNot(
                application.getId(), com.koperasi.loan.domain.enums.InstallmentStatus.PAID)).thenReturn(0L);
        when(loanApplicationRepository.saveAndFlush(application)).thenReturn(application);

        loanService.markInstallmentPaid(installment.getId(), Instant.now());

        assertThat(application.getStatus()).isEqualTo(LoanStatus.COMPLETED);
        assertThat(application.getCompletedAt()).isNotNull();
        verify(loanEventPublisher).publishAfterCommit(
                eq(LoanEventType.APPLICATION_COMPLETED),
                eq(application.getId().toString()),
                anyMap());
    }

    private CreateLoanApplicationRequest request() {
        return new CreateLoanApplicationRequest(product.getId(), AMOUNT, TENOR, "Modal usaha");
    }

    private LoanProduct product() {
        LoanProduct loanProduct = new LoanProduct();
        loanProduct.setId(1);
        loanProduct.setName("Modal Usaha Mikro");
        loanProduct.setMinAmount(new BigDecimal("1000000.00"));
        loanProduct.setMaxAmount(new BigDecimal("50000000.00"));
        loanProduct.setInterestRateAnnual(new BigDecimal("18.00"));
        loanProduct.setMinTenorMonths(3);
        loanProduct.setMaxTenorMonths(24);
        loanProduct.setActive(true);
        loanProduct.setCreatedAt(Instant.now());
        return loanProduct;
    }

    private LoanApplication application(LoanStatus status, Long version) {
        LoanApplication application = LoanApplication.create(
                borrower.userId(), product, AMOUNT, TENOR, "Modal usaha", "key-" + UUID.randomUUID());
        application.setStatus(status);
        application.setVersion(version);
        return application;
    }

    private CreditScore score(LoanApplication application) {
        CreditScore score = CreditScore.create(application, borrower.userId());
        score.applyResult(
                new BigDecimal("78.50"),
                RiskCategory.MEDIUM,
                ApprovalRecommendation.REVIEW,
                Map.of("payment_consistency_score", 0.8),
                "v2.3.1",
                420,
                Instant.now()
        );
        return score;
    }

    private ScoringCompletedEvent scoringEvent(LoanApplication application) {
        return scoringEventFor(application.getId());
    }

    private ScoringCompletedEvent scoringEventFor(UUID applicationId) {
        return new ScoringCompletedEvent(
                "event-1",
                "loan.scoring.completed",
                Instant.now().toString(),
                "trace-1",
                "1.0",
                new ScoringCompletedEvent.ScoringPayload(
                        applicationId,
                        borrower.userId(),
                        new BigDecimal("78.50"),
                        RiskCategory.MEDIUM,
                        ApprovalRecommendation.REVIEW,
                        new BigDecimal("0.91"),
                        Map.of("payment_consistency_score", 0.8),
                        "v2.3.1",
                        420,
                        "SUCCESS",
                        Instant.now()
                )
        );
    }

    private JwtPrincipal principal(UUID userId, String role) {
        return new JwtPrincipal(userId, userId + "@example.id", Set.of(role), "jti-" + userId,
                Instant.now().plusSeconds(900));
    }
}
