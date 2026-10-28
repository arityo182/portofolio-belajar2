package com.koperasi.loan.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.loan.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoanScoringConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoanScoringConsumer.class);

    private final ObjectMapper objectMapper;
    private final LoanService loanService;

    @KafkaListener(
            topics = "${app.kafka.topics.scoring-completed:loan.scoring.completed}",
            groupId = "${spring.kafka.consumer.group-id:loan-service-group}"
    )
    public void consume(String message) {
        ScoringCompletedEvent event;
        try {
            event = objectMapper.readValue(message, ScoringCompletedEvent.class);
        } catch (Exception ex) {
            log.error("Gagal membaca event loan.scoring.completed: {}", ex.getMessage());
            return;
        }

        if (event.payload() == null || event.payload().loanApplicationId() == null) {
            log.warn("Event loan.scoring.completed tidak memiliki loanApplicationId, diabaikan");
            return;
        }

        MDC.put("traceId", event.traceId() == null ? UUID.randomUUID().toString() : event.traceId());
        MDC.put("serviceId", "loan-service");
        try {
            loanService.handleScoringCompleted(event);
            log.info("Event loan.scoring.completed diproses loanApplicationId={}",
                    event.payload().loanApplicationId());
        } finally {
            MDC.remove("traceId");
            MDC.remove("serviceId");
        }
    }
}
