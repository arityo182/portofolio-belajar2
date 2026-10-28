package com.koperasi.notification.service;

import com.koperasi.notification.config.NotificationProperties;
import com.koperasi.notification.domain.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private final NotificationProperties notificationProperties;
    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(UUID userId) {
        SseEmitter emitter = new SseEmitter(notificationProperties.getSseTimeoutMs());
        emitters.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("userId", userId.toString())));
        } catch (IOException ex) {
            remove.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    public void sendNotification(UUID userId, NotificationResponse notification) {
        broadcast(userId, "notification", notification);
    }

    @Scheduled(fixedDelayString = "${app.notification.sse-heartbeat-ms:30000}")
    public void sendHeartbeat() {
        Set<SseEmitter> userEmitters = emitters.keySet().stream()
                .flatMap(userId -> emitters.getOrDefault(userId, Set.of()).stream())
                .collect(java.util.stream.Collectors.toSet());
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("ts", Instant.now().toString())));
            } catch (IOException | IllegalStateException ex) {
                removeEmitter(emitter);
            }
        }
    }

    public int connectedCount(UUID userId) {
        return emitters.getOrDefault(userId, Set.of()).size();
    }

    private void broadcast(UUID userId, String eventName, Object data) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : Set.copyOf(userEmitters)) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                remove(userId, emitter);
            }
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    private void removeEmitter(SseEmitter emitter) {
        emitters.forEach((userId, userEmitters) -> {
            if (userEmitters.remove(emitter) && userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        });
    }
}
