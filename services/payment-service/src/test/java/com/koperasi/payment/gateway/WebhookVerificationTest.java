package com.koperasi.payment.gateway;

import com.koperasi.payment.config.PaymentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookVerificationTest {

    @Test
    void midtransSignature_validSignature_returnsTrue() {
        PaymentProperties properties = new PaymentProperties();
        properties.getMidtrans().setServerKey("server-key");
        MidtransClient client = new MidtransClient(properties, RestClient.builder());
        String orderId = "payment-1";
        String statusCode = "200";
        String grossAmount = "1375000.00";
        String signature = MidtransClient.sha512Hex(orderId + statusCode + grossAmount + "server-key");

        assertThat(client.verifySignature(orderId, statusCode, grossAmount, signature)).isTrue();
        assertThat(client.verifySignature(orderId, statusCode, grossAmount, "invalid")).isFalse();
        assertThat(client.verifySignature(orderId, statusCode, grossAmount, null)).isFalse();
    }

    @Test
    void midtransSignature_withoutServerKey_returnsFalse() {
        PaymentProperties properties = new PaymentProperties();
        MidtransClient client = new MidtransClient(properties, RestClient.builder());

        assertThat(client.verifySignature("o", "200", "100.00", "signature")).isFalse();
    }

    @Test
    void xenditCallbackToken_validToken_returnsTrue() {
        PaymentProperties properties = new PaymentProperties();
        properties.getXendit().setWebhookToken("callback-token");
        XenditClient client = new XenditClient(properties, RestClient.builder());

        assertThat(client.verifyCallbackToken("callback-token")).isTrue();
        assertThat(client.verifyCallbackToken("wrong")).isFalse();
        assertThat(client.verifyCallbackToken(null)).isFalse();
    }

    @Test
    void xenditCallbackToken_withoutConfiguredToken_returnsFalse() {
        PaymentProperties properties = new PaymentProperties();
        XenditClient client = new XenditClient(properties, RestClient.builder());

        assertThat(client.verifyCallbackToken("anything")).isFalse();
    }
}
