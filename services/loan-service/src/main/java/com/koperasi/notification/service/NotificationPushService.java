package com.koperasi.notification.service;

import com.koperasi.notification.domain.dto.NotificationPushMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SseEmitterService sseEmitterService;

    public void pushLocal(NotificationPushMessage message) {
        String destination = "/topic/notifications/" + message.userId();
        messagingTemplate.convertAndSend(destination, message.notification());
        sseEmitterService.sendNotification(message.userId(), message.notification());
    }
}
