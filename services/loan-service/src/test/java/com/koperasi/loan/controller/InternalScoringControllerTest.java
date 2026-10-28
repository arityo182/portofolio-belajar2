package com.koperasi.loan.controller;

import com.koperasi.loan.domain.dto.ApiResponse;
import com.koperasi.loan.event.ScoringCompletedEvent;
import com.koperasi.loan.exception.ForbiddenOperationException;
import com.koperasi.loan.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class InternalScoringControllerTest {

    private final LoanService loanService = mock(LoanService.class);
    private final InternalScoringController controller =
            new InternalScoringController(loanService, "internal-token");

    @Test
    void receiveScoringCompleted_withValidToken_delegatesToService() {
        ScoringCompletedEvent event = event();

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                controller.receiveScoringCompleted("internal-token", event);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).containsEntry("processed", true);
        verify(loanService).handleScoringCompleted(event);
    }

    @Test
    void receiveScoringCompleted_withInvalidOrMissingToken_throwsForbidden() {
        ScoringCompletedEvent event = event();

        assertThatThrownBy(() -> controller.receiveScoringCompleted("wrong", event))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> controller.receiveScoringCompleted(null, event))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(loanService);
    }

    @Test
    void receiveScoringCompleted_whenTokenNotConfigured_throwsForbidden() {
        InternalScoringController unconfigured = new InternalScoringController(loanService, "");

        assertThatThrownBy(() -> unconfigured.receiveScoringCompleted("anything", event()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private ScoringCompletedEvent event() {
        UUID loanId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();
        return new ScoringCompletedEvent(
                UUID.randomUUID().toString(),
                "loan.scoring.completed",
                Instant.now().toString(),
                UUID.randomUUID().toString(),
                "1.0",
                new ScoringCompletedEvent.ScoringPayload(
                        loanId,
                        borrowerId,
                        new BigDecimal("78.50"),
                        com.koperasi.loan.domain.enums.RiskCategory.MEDIUM,
                        com.koperasi.loan.domain.enums.ApprovalRecommendation.REVIEW,
                        new BigDecimal("0.91"),
                        Map.of("payment_consistency_score", 0.8),
                        "v2.3.1",
                        420,
                        "SUCCESS",
                        Instant.now()
                )
        );
    }
}
