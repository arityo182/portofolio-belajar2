package com.koperasi.payment.event;

import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.config.PaymentRetryConfig;
import com.koperasi.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryConsumer {

    private final PaymentService paymentService;
    private final PaymentRetryPublisher retryPublisher;
    private final PaymentProperties paymentProperties;

    @KafkaListener(
            topics = PaymentRetryConfig.RETRY_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}",
            properties = "spring.json.value.default.type=com.koperasi.payment.event.PaymentRetryMessage")
    public void consume(PaymentRetryMessage message) {
        log.info("Memproses retry pembayaran paymentId={} attempt={}", message.paymentId(), message.attempt());
        try {
            paymentService.retryPayment(message.paymentId());
        } catch (Exception ex) {
            int maxAttempts = paymentProperties.getRetryMaxAttempts();
            if (message.attempt() >= maxAttempts) {
                log.error("Retry pembayaran habis paymentId={} attempt={}; dikirim ke DLT",
                        message.paymentId(), message.attempt(), ex);
                throw new IllegalStateException("Retry maksimum tercapai", ex);
            }
            long backoff = paymentProperties.getRetryBaseBackoffMs()
                    * (long) Math.pow(2, Math.max(0, message.attempt() - 1));
            PaymentRetryMessage next = new PaymentRetryMessage(
                    message.paymentId(),
                    message.installmentId(),
                    message.attempt() + 1,
                    ex.getMessage()
            );
            retryPublisher.enqueueDelayed(next, backoff);
            log.warn("Retry pembayaran dijadwalkan ulang paymentId={} attempt={} backoffMs={}",
                    message.paymentId(), next.attempt(), backoff);
        }
    }
}
