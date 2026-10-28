package com.koperasi.auth.event;

import com.koperasi.auth.config.AuditProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditEventPublisher.class);
    private static final String EVENT_TYPE = "audit.event.created";
    private static final String EVENT_VERSION = "1.0";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AuditProperties auditProperties;

    public void publish(AuditPayload payload) {
        doPublish(payload);
    }

    public void publishAfterCommit(AuditPayload payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(payload);
                }
            });
            return;
        }
        doPublish(payload);
    }

    private void doPublish(AuditPayload payload) {
        if (!auditProperties.isEnabled()) {
            return;
        }
        AuditEvent event = new AuditEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                Instant.now().toString(),
                MDC.get("traceId"),
                EVENT_VERSION,
                enrich(payload)
        );
        String key = payload.entityId() == null ? UUID.randomUUID().toString() : payload.entityId();
        try {
            kafkaTemplate.send(auditProperties.getTopic(), key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Gagal mengirim audit event action={} entityId={}: {}",
                                    payload.action(), payload.entityId(), ex.getMessage());
                        }
                    });
        } catch (RuntimeException ex) {
            log.warn("Gagal mengirim audit event action={} entityId={}: {}",
                    payload.action(), payload.entityId(), ex.getMessage());
        }
    }

    private AuditPayload enrich(AuditPayload payload) {
        return new AuditPayload(
                payload.actorId(),
                payload.actorRole(),
                payload.action(),
                payload.entityType(),
                payload.entityId(),
                payload.oldValue(),
                payload.newValue(),
                payload.ipAddress(),
                payload.userAgent(),
                payload.result(),
                auditProperties.getServiceName()
        );
    }
}
