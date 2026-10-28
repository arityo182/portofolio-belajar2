package com.koperasi.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private String websocketAllowedOrigins = "http://localhost:3000";
    private String fanoutChannel = "notifications:fanout";
    private boolean redisFanoutEnabled = true;
    private long sseHeartbeatMs = 30_000;
    private long sseTimeoutMs = 1_800_000;
    private int maxPageSize = 100;
    private String loanServiceUrl = "http://localhost:8082";
    private String internalToken = "";
}
