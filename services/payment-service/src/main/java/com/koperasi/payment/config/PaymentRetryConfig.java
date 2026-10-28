package com.koperasi.payment.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Retry transport for failed gateway payment-link creation. RabbitMQ was replaced by a
 * Kafka topic plus an in-process scheduler for exponential backoff; exhausted retries are
 * published to {@code payment.retry.DLT}.
 */
@Configuration
public class PaymentRetryConfig {

    public static final String RETRY_TOPIC = "payment.retry";
    public static final String RETRY_DLT = "payment.retry.DLT";

    @Bean
    public TaskScheduler paymentRetryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("payment-retry-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }

    @Bean
    public DefaultErrorHandler paymentKafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(RETRY_DLT, -1));
        recoverer.setVerifyPartition(false);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }
}
