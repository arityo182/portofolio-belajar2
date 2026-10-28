package com.koperasi.notification.domain.dto;

import java.util.UUID;

public record NotificationPushMessage(UUID userId, NotificationResponse notification) {
}
