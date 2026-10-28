package com.koperasi.payment.domain.entity;

import com.koperasi.payment.domain.enums.PaymentGateway;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "webhook_events", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class WebhookEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_key", nullable = false, unique = true, length = 255)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false, length = 20)
    private PaymentGateway gateway;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public static WebhookEvent create(String eventKey, PaymentGateway gateway, String externalId,
                                      String eventType, Map<String, Object> payload, boolean signatureValid) {
        WebhookEvent event = new WebhookEvent();
        event.setId(UUID.randomUUID());
        event.setEventKey(eventKey);
        event.setGateway(gateway);
        event.setExternalId(externalId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setSignatureValid(signatureValid);
        event.setReceivedAt(Instant.now());
        return event;
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
        this.processingError = null;
    }

    public void markFailed(String error) {
        this.processed = false;
        this.processingError = error;
    }

    @PreUpdate
    void onUpdate() {
        // no-op placeholder so Hibernate includes the entity in dirty checking semantics
    }
}
