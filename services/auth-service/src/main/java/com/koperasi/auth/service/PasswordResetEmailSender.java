package com.koperasi.auth.service;

import com.koperasi.auth.config.PasswordResetProperties;
import com.koperasi.auth.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEmailSender {

    private static final String EXCHANGE = "notification.exchange";
    private static final String ROUTING_KEY = "notification.email";

    private final RabbitTemplate rabbitTemplate;
    private final PasswordResetProperties properties;

    public void send(User user, String rawToken) {
        String base = properties.getResetUrl();
        String separator = base.contains("?") ? "&" : "?";
        String resetLink = base + separator + "token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        if (properties.isLogLink()) {
            log.info("LOCAL PASSWORD RESET LINK userId={} link={}", user.getId(), resetLink);
        }

        EmailMessage message = new EmailMessage(
                user.getEmail(),
                "Atur ulang kata sandi Koperasi Digital",
                "Kami menerima permintaan untuk mengatur ulang kata sandi akun Anda. "
                        + "Buka tautan berikut dalam 30 menit: " + resetLink
                        + "\n\nJika Anda tidak meminta perubahan ini, abaikan email ini.",
                user.getId().toString()
        );
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message);
        } catch (RuntimeException ex) {
            log.warn("Gagal mengirim email reset kata sandi userId={}: {}", user.getId(), ex.getMessage());
        }
    }

    public record EmailMessage(String to, String subject, String body, String referenceId) {
    }
}
