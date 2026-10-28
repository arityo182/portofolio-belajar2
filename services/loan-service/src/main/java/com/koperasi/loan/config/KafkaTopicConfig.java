package com.koperasi.loan.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares the loan lifecycle topics with a partition count suitable for the demo
 * stack. Production deployments should raise the replication factor to at least 2.
 */
@Configuration
public class KafkaTopicConfig {

    private static final String[] TOPICS = {
            "loan.application.created",
            "loan.application.under_review",
            "loan.application.approved",
            "loan.application.rejected",
            "loan.application.disbursed",
            "loan.application.active",
            "loan.application.completed",
            "loan.installment.overdue",
            "loan.defaulted",
            "loan.scoring.requested",
            "loan.scoring.completed",
            "loan.status.changed",
            "payment.disbursement.initiated",
            "payment.disbursement.completed",
            "payment.disbursement.failed",
            "payment.installment.received",
            "payment.installment.failed",
            "notification.send.requested"
    };

    @Bean
    public KafkaAdmin.NewTopics loanTopics() {
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
