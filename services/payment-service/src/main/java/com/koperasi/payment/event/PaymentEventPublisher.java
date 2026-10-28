package com.koperasi.payment.event;

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
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private static final String VERSION = "1.0";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAfterCommit(String eventType, String key, Map<String, Object> payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(eventType, key, payload);
                }
            });
            return;
        }
        publish(eventType, key, payload);
    }

    public void publish(String eventType, String key, Map<String, Object> payload) {
        PaymentEvent event = new PaymentEvent(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                MDC.get("traceId"),
                VERSION,
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
