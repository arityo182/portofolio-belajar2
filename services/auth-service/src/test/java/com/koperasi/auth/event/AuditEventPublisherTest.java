package com.koperasi.auth.event;

import com.koperasi.auth.config.AuditProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AuditProperties auditProperties;
    private AuditEventPublisher publisher;

    @BeforeEach
    void setUp() {
        auditProperties = new AuditProperties();
        auditProperties.setEnabled(true);
        auditProperties.setTopic("audit.event.created");
        auditProperties.setServiceName("auth-service");
        publisher = new AuditEventPublisher(kafkaTemplate, auditProperties);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publish_sendsEnrichedAuditEventToKafka() {
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("audit.event.created"), eq("entity-1"), any(AuditEvent.class)))
                .thenReturn(future);

        publisher.publish(payload());

        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(kafkaTemplate).send(eq("audit.event.created"), eq("entity-1"), eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.eventType()).isEqualTo("audit.event.created");
        assertThat(event.version()).isEqualTo("1.0");
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.payload().service()).isEqualTo("auth-service");
        assertThat(event.payload().action()).isEqualTo("LOGIN_SUCCESS");
    }

    @Test
    void publish_whenDisabled_doesNotSendEvent() {
        auditProperties.setEnabled(false);

        publisher.publish(payload());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_whenKafkaThrows_doesNotPropagateException() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class)))
                .thenThrow(new IllegalStateException("broker unavailable"));

        assertThatCode(() -> publisher.publish(payload())).doesNotThrowAnyException();
    }

    @Test
    void publishAfterCommit_withoutTransaction_sendsImmediately() {
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        publisher.publishAfterCommit(payload());

        verify(kafkaTemplate).send(eq("audit.event.created"), eq("entity-1"), any(AuditEvent.class));
    }

    @Test
    void publishAfterCommit_withActiveTransaction_sendsOnlyAfterCommit() {
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishAfterCommit(payload());

        verifyNoInteractions(kafkaTemplate);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send(eq("audit.event.created"), eq("entity-1"), any(AuditEvent.class));
    }

    private AuditPayload payload() {
        return new AuditPayload(
                "user-1",
                "BORROWER",
                "LOGIN_SUCCESS",
                "users",
                "entity-1",
                null,
                Map.of("status", "ACTIVE"),
                "127.0.0.1",
                "JUnit",
                "SUCCESS",
                null
        );
    }
}
