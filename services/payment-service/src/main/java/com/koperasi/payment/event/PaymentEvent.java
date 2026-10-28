package com.koperasi.payment.event;

import java.util.Map;

public record PaymentEvent(
        String eventId,
        String eventType,
        String timestamp,
        String traceId,
        String version,
        Map<String, Object> payload
) {
}
