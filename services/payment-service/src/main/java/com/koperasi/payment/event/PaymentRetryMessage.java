package com.koperasi.payment.event;

import java.util.UUID;

public record PaymentRetryMessage(
        UUID paymentId,
        UUID installmentId,
        int attempt,
        String reason
) {
}
