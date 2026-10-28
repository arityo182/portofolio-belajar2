package com.koperasi.auth.event;

public record AuditEvent(
        String eventId,
        String eventType,
        String timestamp,
        String traceId,
        String version,
        AuditPayload payload
) {
}
