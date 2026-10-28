package com.koperasi.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.payment.domain.dto.InstallmentInfo;
import com.koperasi.payment.domain.dto.LoanApplicationInfo;
import com.koperasi.payment.event.PaymentEventPublisher;
import com.koperasi.payment.event.PaymentEventType;
import com.koperasi.payment.event.PaymentRetryPublisher;
import com.koperasi.payment.gateway.DisbursementResult;
import com.koperasi.payment.gateway.MidtransClient;
import com.koperasi.payment.gateway.PaymentLinkResult;
import com.koperasi.payment.gateway.XenditClient;
import com.koperasi.payment.repository.LoanDisbursementRepository;
import com.koperasi.payment.repository.PaymentRepository;
import com.koperasi.payment.service.LoanServiceClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIT {

    private static final String JWT_SECRET = "integration-test-jwt-secret-key-that-is-long-enough-32bytes";
    private static final BigDecimal AMOUNT = new BigDecimal("1375000.00");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("koperasi")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 5);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("jwt.issuer", () -> "koperasi-auth");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:19092");
        registry.add("app.payment.midtrans.server-key", () -> "test-server-key");
        registry.add("app.payment.xendit.webhook-token", () -> "xendit-callback-token");
        registry.add("app.payment.internal-token", () -> "test-internal-token");
        registry.add("logging.level.root", () -> "WARN");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LoanDisbursementRepository loanDisbursementRepository;

    @MockitoBean
    private MidtransClient midtransClient;

    @MockitoBean
    private XenditClient xenditClient;

    @MockitoBean
    private LoanServiceClient loanServiceClient;

    @MockitoBean
    private PaymentRetryPublisher paymentRetryPublisher;

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    @Test
    void payInstallment_createsPendingPaymentAndCachesIdempotentResponse() throws Exception {
        UUID installmentId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();
        UUID loanApplicationId = UUID.randomUUID();
        stubInstallment(installmentId, borrowerId, loanApplicationId);
        when(midtransClient.createPaymentLink(any()))
                .thenReturn(new PaymentLinkResult("https://pay.test/1", "MID-TXN-1"));

        String token = bearer(token(borrowerId, "BORROWER"));
        MvcResult result = mockMvc.perform(post("/api/v1/payments/installments/{id}/pay", installmentId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", "pay-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("MIDTRANS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentGateway").value("MIDTRANS"))
                .andExpect(jsonPath("$.data.paymentUrl").value("https://pay.test/1"))
                .andExpect(jsonPath("$.data.externalTransactionId").value("MID-TXN-1"))
                .andReturn();

        UUID paymentId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("paymentId").asText());
        assertThat(paymentRepository.findById(paymentId)).isPresent();

        mockMvc.perform(post("/api/v1/payments/installments/{id}/pay", installmentId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", "pay-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("MIDTRANS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()));

        verify(midtransClient, times(1)).createPaymentLink(any());
    }

    @Test
    void getInstallmentStatus_returnsPendingThenPaidAfterWebhook() throws Exception {
        UUID installmentId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();
        UUID loanApplicationId = UUID.randomUUID();
        stubInstallment(installmentId, borrowerId, loanApplicationId);
        when(midtransClient.createPaymentLink(any()))
                .thenReturn(new PaymentLinkResult("https://pay.test/2", "MID-TXN-2"));
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> "valid-signature".equals(invocation.getArgument(3)));

        String token = bearer(token(borrowerId, "BORROWER"));
        MvcResult result = mockMvc.perform(post("/api/v1/payments/installments/{id}/pay", installmentId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", "pay-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("MIDTRANS")))
                .andExpect(status().isCreated())
                .andReturn();
        UUID paymentId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("paymentId").asText());

        mockMvc.perform(get("/api/v1/payments/installments/{id}/status", installmentId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.installmentStatus").value("PENDING"));

        mockMvc.perform(post("/api/v1/payments/webhook/midtrans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(midtransWebhook(paymentId, "settlement", "valid-signature")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(true));

        mockMvc.perform(get("/api/v1/payments/installments/{id}/status", installmentId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.installmentStatus").value("PAID"));

        verify(paymentEventPublisher).publishAfterCommit(
                eq(PaymentEventType.INSTALLMENT_RECEIVED), eq(paymentId.toString()), anyMap());
        verify(loanServiceClient).markInstallmentPaid(eq(installmentId), any());
    }

    @Test
    void midtransWebhook_withInvalidSignature_returnsBadRequest() throws Exception {
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/payments/webhook/midtrans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(midtransWebhook(UUID.randomUUID(), "settlement", "bad-signature")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }

    @Test
    void xenditWebhook_withValidCallbackToken_updatesPayment() throws Exception {
        UUID installmentId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();
        UUID loanApplicationId = UUID.randomUUID();
        stubInstallment(installmentId, borrowerId, loanApplicationId);
        when(xenditClient.createPaymentLink(any()))
                .thenReturn(new PaymentLinkResult("https://xendit.test/1", "XEN-INV-1"));
        when(xenditClient.verifyCallbackToken("xendit-callback-token")).thenReturn(true);

        MvcResult result = mockMvc.perform(post("/api/v1/payments/installments/{id}/pay", installmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(borrowerId, "BORROWER")))
                        .header("Idempotency-Key", "xendit-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("XENDIT")))
                .andExpect(status().isCreated())
                .andReturn();
        UUID paymentId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("paymentId").asText());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", "XEN-TXN-1");
        payload.put("external_id", paymentId.toString());
        payload.put("status", "PAID");
        payload.put("amount", AMOUNT);
        payload.put("paid_at", Instant.now().toString());

        mockMvc.perform(post("/api/v1/payments/webhook/xendit")
                        .header("X-Callback-Token", "xendit-callback-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(true));

        mockMvc.perform(get("/api/v1/payments/installments/{id}/status", installmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(borrowerId, "BORROWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));
    }

    @Test
    void xenditWebhook_withInvalidToken_returnsBadRequest() throws Exception {
        when(xenditClient.verifyCallbackToken("wrong")).thenReturn(false);

        mockMvc.perform(post("/api/v1/payments/webhook/xendit")
                        .header("X-Callback-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }

    @Test
    void initiateDisbursement_isIdempotentAndPreventsDoubleDisbursement() throws Exception {
        UUID loanApplicationId = UUID.randomUUID();
        when(loanServiceClient.getLoanApplication(loanApplicationId)).thenReturn(new LoanApplicationInfo(
                loanApplicationId, UUID.randomUUID(), new BigDecimal("10000000.00"), "APPROVED"));
        when(midtransClient.initiateDisbursement(any()))
                .thenReturn(new DisbursementResult("MID-DISB-1", "PENDING"));
        String token = bearer(token(UUID.randomUUID(), "REVIEWER"));
        String body = objectMapper.writeValueAsString(Map.of(
                "loanApplicationId", loanApplicationId.toString(),
                "amount", new BigDecimal("10000000.00"),
                "paymentGateway", "MIDTRANS"
        ));

        MvcResult first = mockMvc.perform(post("/api/v1/payments/disbursements")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", "disb-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        UUID disbursementId = UUID.fromString(
                objectMapper.readTree(first.getResponse().getContentAsString()).path("data").path("disbursementId").asText());

        mockMvc.perform(post("/api/v1/payments/disbursements")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", "disb-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.disbursementId").value(disbursementId.toString()));

        mockMvc.perform(post("/api/v1/payments/disbursements")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", "disb-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        verify(midtransClient, times(1)).initiateDisbursement(any());
        verify(paymentEventPublisher).publishAfterCommit(
                eq(PaymentEventType.DISBURSEMENT_INITIATED), eq(disbursementId.toString()), anyMap());
    }

    @Test
    void midtransWebhook_disbursementSettlement_marksCompletedAndPublishesEvent() throws Exception {
        UUID loanApplicationId = UUID.randomUUID();
        when(loanServiceClient.getLoanApplication(loanApplicationId)).thenReturn(new LoanApplicationInfo(
                loanApplicationId, UUID.randomUUID(), new BigDecimal("10000000.00"), "APPROVED"));
        when(midtransClient.initiateDisbursement(any()))
                .thenReturn(new DisbursementResult("MID-DISB-WEBHOOK", "PENDING"));
        when(midtransClient.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        MvcResult result = mockMvc.perform(post("/api/v1/payments/disbursements")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "REVIEWER")))
                        .header("Idempotency-Key", "disb-webhook-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loanApplicationId", loanApplicationId.toString(),
                                "amount", new BigDecimal("10000000.00"),
                                "paymentGateway", "MIDTRANS"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID disbursementId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .path("data").path("disbursementId").asText());

        mockMvc.perform(post("/api/v1/payments/webhook/midtrans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(midtransWebhook(disbursementId, "settlement", "valid-signature")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(true));

        assertThat(loanDisbursementRepository.findById(disbursementId).orElseThrow().getStatus())
                .isEqualTo(com.koperasi.payment.domain.enums.DisbursementStatus.COMPLETED);
        verify(paymentEventPublisher).publishAfterCommit(
                eq(PaymentEventType.DISBURSEMENT_COMPLETED), eq(disbursementId.toString()), anyMap());
    }

    @Test
    void borrower_cannotInitiateDisbursement() throws Exception {
        mockMvc.perform(post("/api/v1/payments/disbursements")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "BORROWER")))
                        .header("Idempotency-Key", "disb-forbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loanApplicationId", UUID.randomUUID().toString(),
                                "amount", new BigDecimal("10000000.00"),
                                "paymentGateway", "MIDTRANS"
                        ))))
                .andExpect(status().isForbidden());
    }

    private void stubInstallment(UUID installmentId, UUID borrowerId, UUID loanApplicationId) {
        when(loanServiceClient.getInstallment(installmentId)).thenReturn(new InstallmentInfo(
                installmentId,
                loanApplicationId,
                borrowerId,
                1,
                LocalDate.now().plusMonths(1),
                AMOUNT,
                "PENDING",
                null
        ));
        doNothing().when(loanServiceClient).markInstallmentPaid(any(), any());
    }

    private String paymentBody(String gateway) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "paymentGateway", gateway,
                "amount", AMOUNT
        ));
    }

    private String midtransWebhook(UUID paymentId, String transactionStatus, String signature) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "transaction_time", "2026-10-24 10:05:00",
                "transaction_status", transactionStatus,
                "transaction_id", "MID-TXN-WEBHOOK-" + UUID.randomUUID(),
                "order_id", paymentId.toString(),
                "gross_amount", AMOUNT.toPlainString(),
                "signature_key", signature,
                "status_code", "200"
        ));
    }

    private String token(UUID userId, String... roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuer("koperasi-auth")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .claim("type", "access")
                .claim("roles", List.of(roles))
                .claim("email", userId + "@example.id")
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
