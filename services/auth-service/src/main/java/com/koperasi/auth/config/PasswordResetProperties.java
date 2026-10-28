package com.koperasi.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    private long ttlMinutes = 30;
    private String resetUrl = "http://localhost:3000/reset-password";
    private boolean logLink = false;
}
