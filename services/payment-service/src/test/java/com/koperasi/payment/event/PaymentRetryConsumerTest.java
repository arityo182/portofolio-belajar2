package com.koperasi.payment.event;

import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentRetryConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentRetryPublisher retryPublisher;

    private PaymentRetryConsumer consumer;
    private UUID paymentId;
    private UUID installmentId;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        properties.setRetryMaxAttempts(3);
        properties.setRetryBaseBackoffMs(1000);
        consumer = new PaymentRetryConsumer(paymentService, retryPublisher, properties);
        paymentId = UUID.randomUUID();
        installmentId = UUID.randomUUID();
    }

    @Test
    void consume_whenRetrySucceeds_doesNotRepublish() {
        consumer.consume(new PaymentRetryMessage(paymentId, installmentId, 1, "gateway down"));

        verify(paymentService).retryPayment(paymentId);
        verifyNoInteractions(retryPublisher);
    }

    @Test
    void consume_whenAttemptFails_republishesWithExponentialBackoff() {
        doThrow(new IllegalStateException("still down")).when(paymentService).retryPayment(paymentId);

        consumer.consume(new PaymentRetryMessage(paymentId, installmentId, 1, "gateway down"));

        ArgumentCaptor<PaymentRetryMessage> captor = ArgumentCaptor.forClass(PaymentRetryMessage.class);
        verify(retryPublisher).enqueueDelayed(captor.capture(), eq(1000L));
        assertThat(captor.getValue().attempt()).isEqualTo(2);
        assertThat(captor.getValue().paymentId()).isEqualTo(paymentId);
    }

    @Test
    void consume_onSecondFailure_usesLongerBackoff() {
        doThrow(new IllegalStateException("still down")).when(paymentService).retryPayment(paymentId);

        consumer.consume(new PaymentRetryMessage(paymentId, installmentId, 2, "gateway down"));

        verify(retryPublisher).enqueueDelayed(any(PaymentRetryMessage.class), eq(2000L));
    }

    @Test
    void consume_onFinalFailure_throwsForDeadLetterTopic() {
        doThrow(new IllegalStateException("still down")).when(paymentService).retryPayment(paymentId);

        assertThatThrownBy(() -> consumer.consume(
                new PaymentRetryMessage(paymentId, installmentId, 3, "gateway down")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Retry maksimum");

        verify(retryPublisher, never()).enqueueDelayed(any(PaymentRetryMessage.class), anyLong());
    }
}
