package com.koperasi.auth.event;

import java.util.Map;

public record AuditPayload(
        String actorId,
        String actorRole,
        String action,
        String entityType,
        String entityId,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String ipAddress,
        String userAgent,
        String result,
        String service
) {
}
