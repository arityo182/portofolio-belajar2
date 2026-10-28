package com.koperasi.notification.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.koperasi.notification.domain.entity.Notification;
import com.koperasi.notification.domain.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String body,
        NotificationType type,
        String referenceId,
        @JsonProperty("isRead") boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
