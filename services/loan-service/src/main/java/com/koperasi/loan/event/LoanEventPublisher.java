package com.koperasi.loan.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoanEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoanEventPublisher.class);
    private static final String EVENT_VERSION = "1.0";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAfterCommit(String eventType, String key, Map<String, Object> payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(eventType, key, payload);
                }
            });
            return;
        }
        doPublish(eventType, key, payload);
    }

    public void publish(String eventType, String key, Map<String, Object> payload) {
        doPublish(eventType, key, payload);
    }

    private void doPublish(String eventType, String key, Map<String, Object> payload) {
        LoanEvent event = new LoanEvent(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                MDC.get("traceId"),
                EVENT_VERSION,
                payload
        );
        try {
            kafkaTemplate.send(eventType, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Gagal mengirim event {} key={}: {}", eventType, key, ex.getMessage());
                        }
                    });
        } catch (RuntimeException ex) {
            log.warn("Gagal mengirim event {} key={}: {}", eventType, key, ex.getMessage());
        }
    }
}
