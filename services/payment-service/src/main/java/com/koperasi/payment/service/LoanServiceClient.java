package com.koperasi.payment.service;

import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.domain.dto.InstallmentInfo;
import com.koperasi.payment.domain.dto.LoanApplicationInfo;
import com.koperasi.payment.exception.ExternalServiceException;
import com.koperasi.payment.exception.InstallmentNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
public class LoanServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final PaymentProperties properties;
    private final RestClient restClient;

    public LoanServiceClient(PaymentProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        RestClient.Builder builder = restClientBuilder.clone();
        // Keep the payment webhook responsive even when loan-service is unavailable; the
        // reconciliation job retries the idempotent callback later. Tests opt out by setting
        // both timeouts to 0 so MockRestServiceServer can own the request factory.
        if (properties.getLoanConnectTimeoutMs() > 0 || properties.getLoanReadTimeoutMs() > 0) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.getLoanConnectTimeoutMs()))
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofMillis(properties.getLoanReadTimeoutMs()));
            builder.requestFactory(requestFactory);
        }
        this.restClient = builder
                .baseUrl(properties.getLoanServiceUrl())
                .build();
    }

    public InstallmentInfo getInstallment(UUID installmentId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/v1/loans/internal/installments/{id}", installmentId)
                    .header("X-Internal-Token", properties.getInternalToken())
                    .retrieve()
                    .body(MAP_TYPE);
            return toInstallmentInfo(response);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new InstallmentNotFoundException(installmentId);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Gagal mengambil data cicilan dari loan-service", ex);
        }
    }

    public LoanApplicationInfo getLoanApplication(UUID loanApplicationId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/v1/loans/internal/applications/{id}", loanApplicationId)
                    .header("X-Internal-Token", properties.getInternalToken())
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null || !(response.get("data") instanceof Map<?, ?> raw)) {
                throw new ExternalServiceException("Respons loan application tidak valid");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) raw;
            return new LoanApplicationInfo(
                    UUID.fromString(String.valueOf(payload.get("loanApplicationId"))),
                    UUID.fromString(String.valueOf(payload.get("borrowerId"))),
                    payload.get("amount") == null ? null : new BigDecimal(String.valueOf(payload.get("amount"))),
                    payload.get("status") == null ? null : String.valueOf(payload.get("status"))
            );
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ExternalServiceException("Pinjaman tidak ditemukan: " + loanApplicationId, ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Gagal mengambil data pinjaman dari loan-service", ex);
        }
    }

    public void markInstallmentPaid(UUID installmentId, Instant paidAt) {
        try {
            restClient.post()
                    .uri("/api/v1/loans/internal/installments/{id}/paid", installmentId)
                    .header("X-Internal-Token", properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("paidAt", paidAt == null ? Instant.now().toString() : paidAt.toString()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Gagal menandai cicilan lunas di loan-service", ex);
        }
    }

    public void markDisbursed(UUID loanApplicationId, Instant disbursedAt) {
        try {
            restClient.post()
                    .uri("/api/v1/loans/internal/applications/{id}/disbursed", loanApplicationId)
                    .header("X-Internal-Token", properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("disbursedAt",
                            disbursedAt == null ? Instant.now().toString() : disbursedAt.toString()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Gagal menandai pinjaman dicairkan di loan-service", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private InstallmentInfo toInstallmentInfo(Map<String, Object> response) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new ExternalServiceException("Respons installment loan-service tidak valid");
        }
        Map<String, Object> payload = (Map<String, Object>) data;
        return new InstallmentInfo(
                UUID.fromString(String.valueOf(payload.get("installmentId"))),
                UUID.fromString(String.valueOf(payload.get("loanApplicationId"))),
                UUID.fromString(String.valueOf(payload.get("borrowerId"))),
                payload.get("installmentNumber") == null ? null : Integer.valueOf(String.valueOf(payload.get("installmentNumber"))),
                payload.get("dueDate") == null ? null : LocalDate.parse(String.valueOf(payload.get("dueDate"))),
                payload.get("amount") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(payload.get("amount"))),
                payload.get("status") == null ? null : String.valueOf(payload.get("status")),
                payload.get("paidAt") == null ? null : Instant.parse(String.valueOf(payload.get("paidAt")))
        );
    }
}
