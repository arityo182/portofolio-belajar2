package com.koperasi.payment.gateway;

import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.exception.PaymentGatewayException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class XenditClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final PaymentProperties properties;
    private final RestClient restClient;

    public XenditClient(PaymentProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.clone()
                .baseUrl(properties.getXendit().getBaseUrl())
                .build();
    }

    public PaymentLinkResult createPaymentLink(Payment payment) {
        String apiKey = properties.getXendit().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new PaymentGatewayException("Xendit API key belum dikonfigurasi");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("external_id", payment.getId().toString());
            body.put("amount", payment.getAmount().setScale(2, RoundingMode.HALF_UP));
            body.put("description", "Pembayaran cicilan " + payment.getLoanInstallmentId());
            body.put("invoice_duration", 86400);
            body.put("should_send_email", false);

            Map<String, Object> response = restClient.post()
                    .uri("/v2/invoices")
                    .header("Authorization", basicAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null) {
                throw new PaymentGatewayException("Respons Xendit kosong");
            }
            String paymentUrl = stringValue(response.get("invoice_url"));
            String externalId = firstNonBlank(stringValue(response.get("id")), payment.getId().toString());
            return new PaymentLinkResult(paymentUrl, externalId);
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Gagal membuat invoice Xendit", ex);
        }
    }

    public DisbursementResult initiateDisbursement(LoanDisbursement disbursement) {
        String apiKey = properties.getXendit().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new PaymentGatewayException("Xendit API key belum dikonfigurasi");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("external_id", disbursement.getId().toString());
            body.put("amount", disbursement.getAmount().setScale(2, RoundingMode.HALF_UP));
            body.put("bank_code", properties.getXendit().getBankCode());
            body.put("account_holder_name", properties.getXendit().getAccountHolderName());
            body.put("account_number", properties.getXendit().getAccountNumber());
            body.put("description", "Pencairan pinjaman " + disbursement.getLoanApplicationId());

            Map<String, Object> response = restClient.post()
                    .uri("/disbursements")
                    .header("Authorization", basicAuth(apiKey))
                    .header("Idempotency-key", disbursement.getIdempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null) {
                throw new PaymentGatewayException("Respons disbursement Xendit kosong");
            }
            String externalId = firstNonBlank(
                    stringValue(response.get("id")),
                    disbursement.getId().toString()
            );
            return new DisbursementResult(externalId, stringValue(response.get("status")));
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Gagal menginisiasi pencairan Xendit", ex);
        }
    }

    public boolean verifyCallbackToken(String callbackToken) {
        String expected = properties.getXendit().getWebhookToken();
        if (expected == null || expected.isBlank() || callbackToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                callbackToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String basicAuth(String apiKey) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
