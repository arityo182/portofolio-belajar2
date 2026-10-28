package com.koperasi.loan.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlingConfig {

    /**
     * Retries transient listener failures twice, then publishes the record to <topic>.DLT
     * so a poison message cannot block the partition.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DefaultErrorHandler(deadLetterRecoverer(kafkaTemplate),
                new FixedBackOff(1000L, 2L));
    }

    static DeadLetterPublishingRecoverer deadLetterRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, KafkaErrorHandlingConfig::deadLetterDestination);
        recoverer.setVerifyPartition(false);
        return recoverer;
    }

    static TopicPartition deadLetterDestination(
            org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, Exception exception) {
        return new TopicPartition(record.topic() + ".DLT", -1);
    }
}
