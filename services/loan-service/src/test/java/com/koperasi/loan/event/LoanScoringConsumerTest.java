package com.koperasi.loan.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.loan.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LoanScoringConsumerTest {

    @Mock
    private LoanService loanService;

    private ObjectMapper objectMapper;
    private LoanScoringConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new LoanScoringConsumer(objectMapper, loanService);
    }

    @Test
    void consume_withValidEvent_delegatesToLoanService() throws Exception {
        String message = eventJson(UUID.randomUUID(), UUID.randomUUID());

        consumer.consume(message);

        verify(loanService).handleScoringCompleted(any(ScoringCompletedEvent.class));
    }

    @Test
    void consume_withMalformedJson_ignoresEvent() {
        assertThatCode(() -> consumer.consume("not-json")).doesNotThrowAnyException();

        verifyNoInteractions(loanService);
    }

    @Test
    void consume_withoutPayload_ignoresEvent() throws Exception {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "loan.scoring.completed");
        event.put("timestamp", Instant.now().toString());
        event.put("version", "1.0");

        consumer.consume(objectMapper.writeValueAsString(event));

        verifyNoInteractions(loanService);
    }

    @Test
    void consume_whenServiceThrows_propagatesForRetry() throws Exception {
        String message = eventJson(UUID.randomUUID(), UUID.randomUUID());
        org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
                .when(loanService).handleScoringCompleted(any(ScoringCompletedEvent.class));

        assertThatThrownBy(() -> consumer.consume(message))
                .isInstanceOf(IllegalStateException.class);
    }

    private String eventJson(UUID loanId, UUID borrowerId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loanApplicationId", loanId.toString());
        payload.put("borrowerId", borrowerId.toString());
        payload.put("riskScore", 78.50);
        payload.put("riskCategory", "MEDIUM");
        payload.put("approvalRecommendation", "REVIEW");
        payload.put("confidence", 0.91);
        payload.put("features", Map.of("payment_consistency_score", 0.8));
        payload.put("modelVersion", "v2.3.1");
        payload.put("processingTimeMs", 420);
        payload.put("scoringStatus", "SUCCESS");
        payload.put("scoredAt", Instant.now().toString());

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "loan.scoring.completed");
        event.put("timestamp", Instant.now().toString());
        event.put("traceId", UUID.randomUUID().toString());
        event.put("version", "1.0");
        event.put("payload", payload);
        return objectMapper.writeValueAsString(event);
    }
}
