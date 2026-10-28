package com.koperasi.payment.event;

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
class PaymentEventPublisherTest {

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
        PaymentEventPublisher publisher = new PaymentEventPublisher(kafkaTemplate);
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("payment.installment.received"), eq("payment-1"), any(PaymentEvent.class)))
                .thenReturn(future);

        publisher.publish("payment.installment.received", "payment-1", Map.of("status", "SUCCESS"));

        ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(kafkaTemplate).send(eq("payment.installment.received"), eq("payment-1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("payment.installment.received");
        assertThat(captor.getValue().payload()).containsEntry("status", "SUCCESS");
    }

    @Test
    void publish_whenKafkaThrows_doesNotPropagate() {
        PaymentEventPublisher publisher = new PaymentEventPublisher(kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
                .thenThrow(new IllegalStateException("broker down"));

        assertThatCode(() -> publisher.publish("payment.installment.failed", "p-1", Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void publishAfterCommit_withoutTransaction_sendsImmediately() {
        PaymentEventPublisher publisher = new PaymentEventPublisher(kafkaTemplate);
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class))).thenReturn(future);

        publisher.publishAfterCommit("payment.disbursement.initiated", "d-1", Map.of());

        verify(kafkaTemplate).send(eq("payment.disbursement.initiated"), eq("d-1"), any(PaymentEvent.class));
    }

    @Test
    void publishAfterCommit_withTransaction_waitsForCommit() {
        PaymentEventPublisher publisher = new PaymentEventPublisher(kafkaTemplate);
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class))).thenReturn(future);
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishAfterCommit("payment.disbursement.completed", "d-1", Map.of());
        verifyNoInteractions(kafkaTemplate);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send(eq("payment.disbursement.completed"), eq("d-1"), any(PaymentEvent.class));
    }
}
