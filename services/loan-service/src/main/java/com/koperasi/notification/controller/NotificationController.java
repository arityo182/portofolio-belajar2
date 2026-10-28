package com.koperasi.notification.controller;

import com.koperasi.notification.domain.dto.ApiResponse;
import com.koperasi.notification.domain.dto.MarkAllReadResponse;
import com.koperasi.notification.domain.dto.MarkReadResponse;
import com.koperasi.notification.domain.dto.PagedNotificationResponse;
import com.koperasi.notification.domain.dto.UnreadCountResponse;
import com.koperasi.loan.security.JwtPrincipal;
import com.koperasi.notification.service.NotificationService;
import com.koperasi.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedNotificationResponse>> list(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.list(principal.userId(), isRead, page, size), "OK"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.unreadCount(principal.userId()), "OK"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<MarkReadResponse>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.markRead(principal.userId(), id), "Notifikasi ditandai dibaca"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllRead(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.markAllRead(principal.userId()), "Semua notifikasi ditandai dibaca"));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal JwtPrincipal principal) {
        return sseEmitterService.register(principal.userId());
    }
}
