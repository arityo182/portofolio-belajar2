package com.koperasi.loan.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlingConfigTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void poisonRecord_isPublishedToDeadLetterTopic() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        DeadLetterPublishingRecoverer recoverer =
                KafkaErrorHandlingConfig.deadLetterRecoverer(kafkaTemplate);
        recoverer.setFailIfSendResultIsError(false);
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "loan.scoring.completed", 0, 7L, "loan-1",
                Map.of("loanApplicationId", "loan-1"));

        recoverer.accept(record, mock(Consumer.class),
                new IllegalStateException("poison payload"));

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo("loan.scoring.completed.DLT");
    }
}
