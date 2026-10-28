package com.koperasi.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares payment topics with three partitions. The local compose stack runs a single
 * broker, so replication stays at one; production should use at least two replicas.
 */
@Configuration
public class KafkaTopicConfig {

    private static final String[] TOPICS = {
            "payment.disbursement.initiated",
            "payment.disbursement.completed",
            "payment.disbursement.failed",
            "payment.installment.received",
            "payment.installment.failed",
            "payment.retry",
            "payment.retry.DLT"
    };

    @Bean
    public KafkaAdmin.NewTopics paymentTopics() {
        NewTopic[] topics = new NewTopic[TOPICS.length];
        for (int index = 0; index < TOPICS.length; index++) {
            topics[index] = TopicBuilder.name(TOPICS[index])
                    .partitions(3)
                    .replicas(1)
                    .build();
        }
        return new KafkaAdmin.NewTopics(topics);
    }
}
