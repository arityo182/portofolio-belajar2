package com.koperasi.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.notification.config.NotificationProperties;
import com.koperasi.notification.domain.dto.NotificationPushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFanoutService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationPushService notificationPushService;
    private final NotificationProperties notificationProperties;

    public void publish(NotificationPushMessage message) {
        if (!notificationProperties.isRedisFanoutEnabled()) {
            notificationPushService.pushLocal(message);
            return;
        }
        try {
            redisTemplate.convertAndSend(
                    notificationProperties.getFanoutChannel(),
                    objectMapper.writeValueAsString(message)
            );
        } catch (Exception ex) {
            log.warn("Redis fanout gagal, push lokal saja userId={}: {}", message.userId(), ex.getMessage());
            notificationPushService.pushLocal(message);
        }
    }

    public void onMessage(String payload) {
        try {
            NotificationPushMessage message = objectMapper.readValue(payload, NotificationPushMessage.class);
            notificationPushService.pushLocal(message);
        } catch (Exception ex) {
            log.warn("Pesan fanout notifikasi tidak valid: {}", ex.getMessage());
        }
    }
}
