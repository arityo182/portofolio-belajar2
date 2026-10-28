package com.koperasi.notification.service;

import com.koperasi.notification.domain.dto.MarkAllReadResponse;
import com.koperasi.notification.domain.dto.MarkReadResponse;
import com.koperasi.notification.domain.dto.NotificationResponse;
import com.koperasi.notification.domain.dto.PagedNotificationResponse;
import com.koperasi.notification.domain.dto.UnreadCountResponse;
import com.koperasi.notification.domain.entity.Notification;
import com.koperasi.notification.domain.enums.NotificationType;
import com.koperasi.loan.exception.BusinessValidationException;
import com.koperasi.loan.exception.ResourceNotFoundException;
import com.koperasi.notification.repository.NotificationRepository;
import com.koperasi.notification.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProperties notificationProperties;

    @Transactional
    public Optional<Notification> create(UUID userId, String title, String body, NotificationType type,
                                         String referenceId, String sourceEventId) {
        if (userId == null) {
            throw new BusinessValidationException("userId notifikasi wajib diisi");
        }
        if (sourceEventId != null && !sourceEventId.isBlank()
                && notificationRepository.existsBySourceEventId(sourceEventId)) {
            return Optional.empty();
        }
        Notification notification = Notification.create(
                userId,
                truncate(title, 200),
                body == null ? "" : body,
                type == null ? NotificationType.SYSTEM : type,
                truncate(referenceId, 100),
                truncate(sourceEventId, 100)
        );
        try {
            return Optional.of(notificationRepository.saveAndFlush(notification));
        } catch (DataIntegrityViolationException ex) {
            log.info("Notifikasi duplikat untuk sourceEventId={}", sourceEventId);
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public PagedNotificationResponse list(UUID userId, Boolean isRead, int page, int size) {
        if (page < 0) {
            throw new BusinessValidationException("Parameter page tidak boleh negatif");
        }
        if (size < 1 || size > notificationProperties.getMaxPageSize()) {
            throw new BusinessValidationException("Parameter size harus antara 1 dan "
                    + notificationProperties.getMaxPageSize());
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = isRead == null
                ? notificationRepository.findByUserId(userId, pageable)
                : notificationRepository.findByUserIdAndRead(userId, isRead, pageable);
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
        return new PagedNotificationResponse(
                result.getContent().stream().map(NotificationResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                unreadCount
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(
                notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public MarkReadResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notifikasi tidak ditemukan: " + notificationId));
        notification.markRead();
        notificationRepository.save(notification);
        return new MarkReadResponse(notification.getId(), notification.isRead());
    }

    @Transactional
    public MarkAllReadResponse markAllRead(UUID userId) {
        int marked = notificationRepository.markAllRead(userId, Instant.now());
        return new MarkAllReadResponse(marked);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
