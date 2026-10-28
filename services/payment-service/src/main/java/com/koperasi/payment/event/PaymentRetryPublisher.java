package com.koperasi.payment.event;

import com.koperasi.payment.config.PaymentRetryConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentRetryPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TaskScheduler paymentRetryScheduler;

    public void enqueueFailedPayment(UUID paymentId, UUID installmentId, String reason) {
        send(new PaymentRetryMessage(paymentId, installmentId, 1, reason));
    }

    public void enqueueFailedPaymentAfterCommit(UUID paymentId, UUID installmentId, String reason) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        enqueueFailedPayment(paymentId, installmentId, reason);
                    } catch (RuntimeException ex) {
                        log.error("Gagal mengirim pesan retry pembayaran paymentId={}", paymentId, ex);
                    }
                }
            });
            return;
        }
        enqueueFailedPayment(paymentId, installmentId, reason);
    }

    public void enqueueDelayed(PaymentRetryMessage message, long delayMs) {
        paymentRetryScheduler.schedule(
                () -> send(message),
                Instant.now().plusMillis(Math.max(1, delayMs)));
    }

    private void send(PaymentRetryMessage message) {
        kafkaTemplate.send(PaymentRetryConfig.RETRY_TOPIC,
                message.paymentId().toString(), message);
    }
}
