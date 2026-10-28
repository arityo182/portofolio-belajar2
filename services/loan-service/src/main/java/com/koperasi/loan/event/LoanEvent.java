package com.koperasi.loan.event;

import java.util.Map;

public record LoanEvent(
        String eventId,
        String eventType,
        String timestamp,
        String traceId,
        String version,
        Map<String, Object> payload
) {
}
