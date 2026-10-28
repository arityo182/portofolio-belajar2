package com.koperasi.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.notification.domain.dto.NotificationPushMessage;
import com.koperasi.notification.domain.dto.NotificationResponse;
import com.koperasi.notification.domain.entity.Notification;
import com.koperasi.notification.domain.enums.NotificationType;
import com.koperasi.notification.service.NotificationFanoutService;
import com.koperasi.loan.service.LoanService;
import com.koperasi.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationFanoutService notificationFanoutService;
    private final LoanService loanService;

    @KafkaListener(
            topicPattern = "${app.notification.kafka-topic-pattern}",
            groupId = "${spring.kafka.consumer.group-id:core-service-group}"
    )
    public void consume(String message) {
        JsonNode root;
        try {
            root = objectMapper.readTree(message);
        } catch (Exception ex) {
            log.error("Gagal membaca event notifikasi: {}", ex.getMessage());
            return;
        }

        String eventType = text(root, "eventType");
        JsonNode payload = root.has("payload") ? root.get("payload") : root;
        if (eventType == null || eventType.isBlank()) {
            log.warn("Event notifikasi tanpa eventType, diabaikan");
            return;
        }

        UUID loanApplicationId = uuid(payload, "loanApplicationId");
        UUID recipient = uuid(payload, "userId", "borrowerId", "recipientId", "investorId");
        if (recipient == null && loanApplicationId != null) {
            recipient = resolveBorrower(loanApplicationId);
        }
        if (recipient == null) {
            log.debug("Event {} tidak memiliki penerima yang dapat dipetakan, diabaikan", eventType);
            return;
        }

        String referenceId = firstText(payload, "loanApplicationId", "paymentId", "disbursementId", "referenceId");
        String eventId = text(root, "eventId");
        String sourceEventId = firstNonBlank(eventId,
                eventType + "|" + referenceId + "|" + firstText(root, "timestamp"));
        NotificationType type = notificationType(eventType, text(payload, "type"));
        String title = firstNonBlank(text(payload, "title"), titleFor(eventType));
        String body = firstNonBlank(text(payload, "body"), bodyFor(eventType, payload));

        MDC.put("traceId", firstNonBlank(text(root, "traceId"), UUID.randomUUID().toString()));
        MDC.put("serviceId", "core-service");
        try {
            Optional<Notification> saved = notificationService.create(
                    recipient, title, body, type, referenceId, sourceEventId);
            if (saved.isEmpty()) {
                log.debug("Notifikasi duplikat diabaikan sourceEventId={}", sourceEventId);
                return;
            }
            Notification notification = saved.get();
            notificationFanoutService.publish(new NotificationPushMessage(
                    recipient, NotificationResponse.from(notification)));
            log.info("Notifikasi dibuat userId={} type={} referenceId={}",
                    recipient, type, referenceId);
        } catch (DataAccessException ex) {
            log.error("Gagal menyimpan notifikasi event {} untuk userId={}", eventType, recipient, ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Gagal memproses event {} untuk userId={}: {}", eventType, recipient, ex.getMessage());
        } finally {
            MDC.remove("traceId");
            MDC.remove("serviceId");
        }
    }

    private UUID resolveBorrower(UUID loanApplicationId) {
        try {
            return loanService.getApplicationOwner(loanApplicationId).borrowerId();
        } catch (RuntimeException ex) {
            log.debug("Borrower untuk loanApplicationId={} tidak ditemukan: {}",
                    loanApplicationId, ex.getMessage());
            return null;
        }
    }

    private NotificationType notificationType(String eventType, String explicitType) {
        if (explicitType != null) {
            try {
                return NotificationType.valueOf(explicitType.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to event-derived type
            }
        }
        if (eventType.startsWith("payment.")) {
            return NotificationType.PAYMENT;
        }
        if (eventType.startsWith("loan.")) {
            return NotificationType.LOAN_STATUS;
        }
        return NotificationType.SYSTEM;
    }

    private String titleFor(String eventType) {
        return switch (eventType) {
            case "loan.application.created" -> "Pengajuan Pinjaman Diterima";
            case "loan.application.under_review" -> "Pengajuan Sedang Ditinjau";
            case "loan.application.approved" -> "Pinjaman Disetujui";
            case "loan.application.rejected" -> "Pinjaman Ditolak";
            case "loan.application.disbursed" -> "Dana Pinjaman Dicairkan";
            case "loan.application.active" -> "Pinjaman Aktif";
            case "loan.application.completed" -> "Pinjaman Lunas";
            case "loan.installment.overdue" -> "Cicilan Terlambat";
            case "loan.status.changed" -> "Status Pinjaman Diperbarui";
            case "loan.defaulted" -> "Pinjaman Gagal Bayar";
            case "payment.installment.received" -> "Pembayaran Diterima";
            case "payment.installment.failed" -> "Pembayaran Gagal";
            case "payment.disbursement.initiated" -> "Pencairan Diproses";
            case "payment.disbursement.completed" -> "Pencairan Selesai";
            case "payment.disbursement.failed" -> "Pencairan Gagal";
            case "notification.send.requested" -> "Notifikasi Baru";
            default -> "Notifikasi Sistem";
        };
    }

    private String bodyFor(String eventType, JsonNode payload) {
        return switch (eventType) {
            case "loan.application.created" ->
                    "Pengajuan pinjaman Anda telah diterima dan masuk antrian penilaian.";
            case "loan.application.under_review" ->
                    "Pengajuan pinjaman Anda sedang ditinjau oleh tim reviewer.";
            case "loan.application.approved" ->
                    "Selamat! Pengajuan pinjaman Anda telah disetujui.";
            case "loan.application.rejected" ->
                    "Pengajuan pinjaman Anda ditolak."
                            + (text(payload, "rejectionReason") == null
                            ? "" : " Alasan: " + text(payload, "rejectionReason"));
            case "loan.application.disbursed" ->
                    "Dana pinjaman Anda telah dicairkan.";
            case "loan.application.active" ->
                    "Pembayaran pertama diterima. Pinjaman Anda kini aktif.";
            case "loan.application.completed" ->
                    "Seluruh cicilan telah lunas. Pinjaman Anda selesai.";
            case "loan.installment.overdue" ->
                    "Cicilan Anda telah melewati jatuh tempo."
                            + (text(payload, "daysOverdue") == null
                            ? "" : " Keterlambatan " + text(payload, "daysOverdue") + " hari.");
            case "loan.status.changed" ->
                    "Status pinjaman Anda berubah menjadi " + firstNonBlank(text(payload, "status"), "baru") + ".";
            case "loan.defaulted" ->
                    "Pinjaman Anda telah memasuki status gagal bayar. Hubungi tim koperasi.";
            case "payment.installment.received" ->
                    "Pembayaran cicilan Anda telah diterima."
                            + (text(payload, "amount") == null ? "" : " Nominal: " + text(payload, "amount"));
            case "payment.installment.failed" ->
                    "Pembayaran cicilan Anda gagal."
                            + (text(payload, "failureReason") == null
                            ? "" : " Alasan: " + text(payload, "failureReason"));
            case "payment.disbursement.initiated" ->
                    "Proses pencairan pinjaman Anda sedang berjalan.";
            case "payment.disbursement.completed" ->
                    "Pencairan pinjaman Anda telah selesai.";
            case "payment.disbursement.failed" ->
                    "Pencairan pinjaman Anda gagal diproses.";
            default -> "Ada pembaruan terbaru pada akun koperasi Anda.";
        };
    }

    private UUID uuid(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
