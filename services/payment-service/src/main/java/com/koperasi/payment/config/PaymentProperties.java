package com.koperasi.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private long idempotencyTtlHours = 24;
    private long lockTtlSeconds = 15;
    private int retryMaxAttempts = 3;
    private long retryBaseBackoffMs = 1000;

    private String loanServiceUrl = "http://localhost:8082";
    private String internalToken = "";
    private long loanConnectTimeoutMs = 2000;
    private long loanReadTimeoutMs = 3000;

    private Midtrans midtrans = new Midtrans();
    private Xendit xendit = new Xendit();

    public Duration idempotencyTtl() {
        return Duration.ofHours(idempotencyTtlHours);
    }

    public Duration lockTtl() {
        return Duration.ofSeconds(lockTtlSeconds);
    }

    @Getter
    @Setter
    public static class Midtrans {
        private String serverKey = "";
        private String clientKey = "";
        private String snapBaseUrl = "https://app.sandbox.midtrans.com";
        private String apiBaseUrl = "https://api.sandbox.midtrans.com";
        private boolean production = false;
        private String beneficiaryName = "Koperasi Digital";
        private String beneficiaryAccount = "0000000000";
        private String beneficiaryBank = "BCA";
    }

    @Getter
    @Setter
    public static class Xendit {
        private String apiKey = "";
        private String baseUrl = "https://api.xendit.co";
        private String webhookToken = "";
        private String bankCode = "BCA";
        private String accountHolderName = "Koperasi Digital";
        private String accountNumber = "0000000000";
    }
}
