package com.koperasi.loan.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.loan.domain.entity.CreditScore;
import com.koperasi.loan.domain.entity.LoanApplication;
import com.koperasi.loan.domain.entity.LoanProduct;
import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.domain.enums.RiskCategory;
import com.koperasi.loan.event.LoanEventPublisher;
import com.koperasi.loan.event.LoanEventType;
import com.koperasi.loan.event.LoanScoringConsumer;
import com.koperasi.loan.repository.CreditScoreRepository;
import com.koperasi.loan.repository.LoanApplicationRepository;
import com.koperasi.loan.repository.LoanInstallmentRepository;
import com.koperasi.loan.repository.LoanProductRepository;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class LoanControllerIT {

    private static final String JWT_SECRET = "integration-test-jwt-secret-key-that-is-long-enough-32bytes";

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
        registry.add("spring.kafka.listener.auto-startup", () -> false);
        registry.add("spring.kafka.consumer.group-id", () -> "loan-service-it");
        registry.add("app.internal.callback-token", () -> "test-internal-token");
        registry.add("logging.level.root", () -> "WARN");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private LoanInstallmentRepository loanInstallmentRepository;

    @Autowired
    private CreditScoreRepository creditScoreRepository;

    @Autowired
    private LoanScoringConsumer loanScoringConsumer;

    @MockitoBean
    private LoanEventPublisher loanEventPublisher;

    @Test
    void getProducts_returnsSeededActiveProducts() throws Exception {
        mockMvc.perform(get("/api/v1/loans/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data[0].interestRateAnnual").isNotEmpty())
                .andExpect(jsonPath("$.data[0].isActive").value(true));
    }

    @Test
    void applyLoan_createsPendingApplicationAndPublishesRequiredEvents() throws Exception {
        UUID borrowerId = UUID.randomUUID();
        String idempotencyKey = "it-apply-" + UUID.randomUUID();
        LoanProduct product = firstProduct();

        MvcResult result = mockMvc.perform(post("/api/v1/loans/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(borrowerId, "BORROWER")))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.amount").value(10000000.00))
                .andExpect(jsonPath("$.data.tenorMonths").value(12))
                .andExpect(jsonPath("$.data.borrowerId").value(borrowerId.toString()))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();

        UUID loanId = loanId(result);
        verify(loanEventPublisher, times(1)).publishAfterCommit(
                eq(LoanEventType.APPLICATION_CREATED), eq(loanId.toString()), anyMap());
        verify(loanEventPublisher, times(1)).publishAfterCommit(
                eq(LoanEventType.SCORING_REQUESTED), eq(loanId.toString()), anyMap());
    }

    @Test
    void applyLoan_withDuplicateIdempotencyKey_returnsCachedApplication() throws Exception {
        UUID borrowerId = UUID.randomUUID();
        String idempotencyKey = "it-idem-" + UUID.randomUUID();
        LoanProduct product = firstProduct();
        String token = bearer(token(borrowerId, "BORROWER"));

        UUID firstId = loanId(mockMvc.perform(post("/api/v1/loans/applications")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(product.getId())))
                .andExpect(status().isCreated())
                .andReturn());

        UUID secondId = loanId(mockMvc.perform(post("/api/v1/loans/applications")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(product.getId())))
                .andExpect(status().isCreated())
                .andReturn());

        assertThat(secondId).isEqualTo(firstId);
        verify(loanEventPublisher, times(1)).publishAfterCommit(
                eq(LoanEventType.APPLICATION_CREATED), eq(firstId.toString()), anyMap());
    }

    @Test
    void applyLoan_withoutIdempotencyHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/loans/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "BORROWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(firstProduct().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void scoringCompleted_updatesStatusAndSavesCreditScore() throws Exception {
        AppliedLoan loan = applyLoan();

        consumeScoringCompleted(loan.loanId(), loan.borrowerId());

        LoanApplication application = loanApplicationRepository.findById(loan.loanId()).orElseThrow();
        assertThat(application.getStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
        CreditScore score = creditScoreRepository.findByLoanApplicationId(loan.loanId()).orElseThrow();
        assertThat(score.getScore()).isEqualByComparingTo("78.50");
        assertThat(score.getRiskCategory()).isEqualTo(RiskCategory.MEDIUM);
        verify(loanEventPublisher, times(1)).publishAfterCommit(
                eq(LoanEventType.APPLICATION_UNDER_REVIEW), eq(loan.loanId().toString()), anyMap());
    }

    @Test
    void approveLoan_generatesInstallmentsAndMakesScheduleAvailable() throws Exception {
        AppliedLoan loan = applyLoan();
        consumeScoringCompleted(loan.loanId(), loan.borrowerId());
        long version = currentVersion(loan.loanId());

        mockMvc.perform(patch("/api/v1/loans/applications/{id}/status", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "REVIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("APPROVED", null, false, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewerId").isNotEmpty())
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        assertThat(loanInstallmentRepository.countByLoanApplicationId(loan.loanId())).isEqualTo(12);
        verify(loanEventPublisher, times(1)).publishAfterCommit(
                eq(LoanEventType.APPLICATION_APPROVED), eq(loan.loanId().toString()), anyMap());

        mockMvc.perform(get("/api/v1/loans/applications/{id}/installments", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(loan.borrowerId(), "BORROWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(12))
                .andExpect(jsonPath("$.data[0].installmentNumber").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void getScore_afterScoring_returnsScoreForReviewer() throws Exception {
        AppliedLoan loan = applyLoan();
        consumeScoringCompleted(loan.loanId(), loan.borrowerId());

        mockMvc.perform(get("/api/v1/loans/applications/{id}/score", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "REVIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(78.50))
                .andExpect(jsonPath("$.data.riskCategory").value("MEDIUM"))
                .andExpect(jsonPath("$.data.approvalRecommendation").value("REVIEW"))
                .andExpect(jsonPath("$.data.features.payment_consistency_score").value(0.8));

        mockMvc.perform(get("/api/v1/loans/applications/{id}/score", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(loan.borrowerId(), "BORROWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(78.50));

        mockMvc.perform(get("/api/v1/loans/applications/{id}/score", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "BORROWER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectLoan_withoutReason_returnsUnprocessableEntity() throws Exception {
        AppliedLoan loan = applyLoan();
        consumeScoringCompleted(loan.loanId(), loan.borrowerId());

        mockMvc.perform(patch("/api/v1/loans/applications/{id}/status", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "REVIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("REJECTED", null, false, currentVersion(loan.loanId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectLoan_withReason_updatesStatusAndPublishesEvent() throws Exception {
        AppliedLoan loan = applyLoan();
        consumeScoringCompleted(loan.loanId(), loan.borrowerId());

        mockMvc.perform(patch("/api/v1/loans/applications/{id}/status", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "REVIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("REJECTED", "Risiko kredit terlalu tinggi", false,
                                currentVersion(loan.loanId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Risiko kredit terlalu tinggi"));

        verify(loanEventPublisher, times(1)).publishAfterCommit(
                eq(LoanEventType.APPLICATION_REJECTED), eq(loan.loanId().toString()), anyMap());
    }

    @Test
    void updateStatus_withStaleVersion_returnsConflict() throws Exception {
        AppliedLoan loan = applyLoan();
        consumeScoringCompleted(loan.loanId(), loan.borrowerId());

        mockMvc.perform(patch("/api/v1/loans/applications/{id}/status", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "REVIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("APPROVED", null, false, 99L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void borrower_cannotChangeLoanStatus() throws Exception {
        AppliedLoan loan = applyLoan();

        mockMvc.perform(patch("/api/v1/loans/applications/{id}/status", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(loan.borrowerId(), "BORROWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("APPROVED", null, false, currentVersion(loan.loanId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getApplication_asOtherBorrower_returnsForbidden() throws Exception {
        AppliedLoan loan = applyLoan();

        mockMvc.perform(get("/api/v1/loans/applications/{id}", loan.loanId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(UUID.randomUUID(), "BORROWER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalScoringCallback_withValidToken_updatesStatus() throws Exception {
        AppliedLoan loan = applyLoan();
        String event = scoringEventJson(loan.loanId(), loan.borrowerId());

        mockMvc.perform(post("/api/v1/loans/internal/scoring/completed")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(true));

        LoanApplication application = loanApplicationRepository.findById(loan.loanId()).orElseThrow();
        assertThat(application.getStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
        assertThat(creditScoreRepository.findByLoanApplicationId(loan.loanId())).isPresent();
    }

    @Test
    void internalScoringCallback_withInvalidToken_returnsForbidden() throws Exception {
        AppliedLoan loan = applyLoan();

        mockMvc.perform(post("/api/v1/loans/internal/scoring/completed")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scoringEventJson(loan.loanId(), loan.borrowerId())))
                .andExpect(status().isForbidden());

        LoanApplication application = loanApplicationRepository.findById(loan.loanId()).orElseThrow();
        assertThat(application.getStatus()).isEqualTo(LoanStatus.PENDING);
    }

    private AppliedLoan applyLoan() throws Exception {
        UUID borrowerId = UUID.randomUUID();
        LoanProduct product = firstProduct();
        MvcResult result = mockMvc.perform(post("/api/v1/loans/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(borrowerId, "BORROWER")))
                        .header("Idempotency-Key", "it-loan-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody(product.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        return new AppliedLoan(loanId(result), borrowerId);
    }

    private void consumeScoringCompleted(UUID loanId, UUID borrowerId) throws Exception {
        loanScoringConsumer.consume(scoringEventJson(loanId, borrowerId));
    }

    private String scoringEventJson(UUID loanId, UUID borrowerId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loanApplicationId", loanId.toString());
        payload.put("borrowerId", borrowerId.toString());
        payload.put("riskScore", new BigDecimal("78.50"));
        payload.put("riskCategory", "MEDIUM");
        payload.put("approvalRecommendation", "REVIEW");
        payload.put("confidence", new BigDecimal("0.91"));
        payload.put("features", Map.of("payment_consistency_score", 0.8));
        payload.put("modelVersion", "v2.3.1");
        payload.put("processingTimeMs", 420);
        payload.put("scoringStatus", "SUCCESS");
        payload.put("scoredAt", Instant.now().toString());

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "loan.scoring.completed");
        event.put("timestamp", Instant.now().toString());
        event.put("traceId", UUID.randomUUID().toString());
        event.put("version", "1.0");
        event.put("payload", payload);
        return objectMapper.writeValueAsString(event);
    }

    private long currentVersion(UUID loanId) {
        return loanApplicationRepository.findById(loanId).orElseThrow().getVersion();
    }

    private LoanProduct firstProduct() {
        return loanProductRepository.findByActiveTrueOrderByMinAmountAsc().get(0);
    }

    private UUID loanId(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return UUID.fromString(data.path("loanId").asText());
    }

    private String applyBody(Integer productId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "loanProductId", productId,
                "amount", new BigDecimal("10000000.00"),
                "tenorMonths", 12,
                "purpose", "Pembelian peralatan dapur untuk ekspansi warung"
        ));
    }

    private String statusBody(String status, String rejectionReason, boolean overrideScore, long version)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        if (rejectionReason != null) {
            body.put("rejectionReason", rejectionReason);
        }
        body.put("overrideScore", overrideScore);
        body.put("version", version);
        return objectMapper.writeValueAsString(body);
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

    private record AppliedLoan(UUID loanId, UUID borrowerId) {
    }
}
