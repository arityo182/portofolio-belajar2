package com.koperasi.notification.service;

import com.koperasi.notification.service.NotificationFanoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber implements MessageListener {

    private final NotificationFanoutService notificationFanoutService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        notificationFanoutService.onMessage(new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
