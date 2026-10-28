package com.koperasi.loan.event;

import org.junit.jupiter.api.AfterEach;
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
class LoanEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publish_sendsEventToTopicNamedByEventType() {
        LoanEventPublisher eventPublisher = new LoanEventPublisher(kafkaTemplate);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("loan.application.created"), eq("loan-1"), any(LoanEvent.class))).thenReturn(future);

        eventPublisher.publish("loan.application.created", "loan-1", Map.of("status", "PENDING"));

        ArgumentCaptor<LoanEvent> captor = ArgumentCaptor.forClass(LoanEvent.class);
        verify(kafkaTemplate).send(eq("loan.application.created"), eq("loan-1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("loan.application.created");
        assertThat(captor.getValue().version()).isEqualTo("1.0");
        assertThat(captor.getValue().payload()).containsEntry("status", "PENDING");
    }

    @Test
    void publish_whenKafkaThrows_doesNotPropagate() {
        LoanEventPublisher eventPublisher = new LoanEventPublisher(kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), any(LoanEvent.class)))
                .thenThrow(new IllegalStateException("broker down"));

        assertThatCode(() -> eventPublisher.publish("loan.application.approved", "loan-1", Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void publishAfterCommit_withoutTransaction_sendsImmediately() {
        LoanEventPublisher eventPublisher = new LoanEventPublisher(kafkaTemplate);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(LoanEvent.class))).thenReturn(future);

        eventPublisher.publishAfterCommit("loan.application.created", "loan-1", Map.of());

        verify(kafkaTemplate).send(eq("loan.application.created"), eq("loan-1"), any(LoanEvent.class));
    }

    @Test
    void publishAfterCommit_withTransaction_waitsForCommit() {
        LoanEventPublisher eventPublisher = new LoanEventPublisher(kafkaTemplate);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(LoanEvent.class))).thenReturn(future);
        TransactionSynchronizationManager.initSynchronization();

        eventPublisher.publishAfterCommit("loan.application.created", "loan-1", Map.of());

        verifyNoInteractions(kafkaTemplate);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send(eq("loan.application.created"), eq("loan-1"), any(LoanEvent.class));
    }
}
