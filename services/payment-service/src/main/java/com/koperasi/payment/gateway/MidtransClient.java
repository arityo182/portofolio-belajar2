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
public class MidtransClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final PaymentProperties properties;
    private final RestClient snapClient;
    private final RestClient apiClient;

    public MidtransClient(PaymentProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.snapClient = restClientBuilder.clone()
                .baseUrl(properties.getMidtrans().getSnapBaseUrl())
                .build();
        this.apiClient = restClientBuilder.clone()
                .baseUrl(properties.getMidtrans().getApiBaseUrl())
                .build();
    }

    public PaymentLinkResult createPaymentLink(Payment payment) {
        String serverKey = properties.getMidtrans().getServerKey();
        if (serverKey == null || serverKey.isBlank()) {
            throw new PaymentGatewayException("Midtrans server key belum dikonfigurasi");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("transaction_details", Map.of(
                    "order_id", payment.getId().toString(),
                    "gross_amount", payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
            ));
            body.put("customer_details", Map.of(
                    "first_name", "Borrower",
                    "email", "borrower-" + payment.getBorrowerId() + "@koperasi.local"
            ));
            body.put("callbacks", Map.of("finish", properties.getLoanServiceUrl() + "/payment-finish"));

            Map<String, Object> response = snapClient.post()
                    .uri("/snap/v1/transactions")
                    .header("Authorization", basicAuth(serverKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null) {
                throw new PaymentGatewayException("Respons Midtrans kosong");
            }
            String paymentUrl = stringValue(response.get("redirect_url"));
            String externalId = firstNonBlank(stringValue(response.get("token")), payment.getId().toString());
            return new PaymentLinkResult(paymentUrl, externalId);
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Gagal membuat payment link Midtrans", ex);
        }
    }

    public DisbursementResult initiateDisbursement(LoanDisbursement disbursement) {
        String serverKey = properties.getMidtrans().getServerKey();
        if (serverKey == null || serverKey.isBlank()) {
            throw new PaymentGatewayException("Midtrans server key belum dikonfigurasi");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("external_id", disbursement.getId().toString());
            body.put("amount", disbursement.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            body.put("beneficiary_name", properties.getMidtrans().getBeneficiaryName());
            body.put("beneficiary_account", properties.getMidtrans().getBeneficiaryAccount());
            body.put("beneficiary_bank", properties.getMidtrans().getBeneficiaryBank());
            body.put("description", "Pencairan pinjaman " + disbursement.getLoanApplicationId());

            Map<String, Object> response = apiClient.post()
                    .uri("/v1/payouts")
                    .header("X-API-Key", serverKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null) {
                throw new PaymentGatewayException("Respons disbursement Midtrans kosong");
            }
            String externalId = firstNonBlank(
                    stringValue(response.get("reference_no")),
                    stringValue(response.get("id")),
                    disbursement.getId().toString()
            );
            return new DisbursementResult(externalId, stringValue(response.get("status")));
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Gagal menginisiasi pencairan Midtrans", ex);
        }
    }

    public boolean verifySignature(String orderId, String statusCode, String grossAmount, String signatureKey) {
        String serverKey = properties.getMidtrans().getServerKey();
        if (serverKey == null || serverKey.isBlank() || signatureKey == null || signatureKey.isBlank()) {
            return false;
        }
        String expected = sha512Hex(orderId + statusCode + grossAmount + serverKey);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureKey.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String sha512Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-512 tidak tersedia", ex);
        }
    }

    private String basicAuth(String serverKey) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((serverKey + ":").getBytes(StandardCharsets.UTF_8));
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
