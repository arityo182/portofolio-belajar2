package com.koperasi.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {

    private boolean enabled = true;
    private String topic = "audit.event.created";
    private String serviceName = "auth-service";
}
