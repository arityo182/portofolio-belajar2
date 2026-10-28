package com.koperasi.payment.service;

import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.domain.dto.InstallmentInfo;
import com.koperasi.payment.domain.dto.LoanApplicationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LoanServiceClientTest {

    private PaymentProperties properties() {
        PaymentProperties properties = new PaymentProperties();
        properties.setLoanServiceUrl("http://loan.test");
        properties.setInternalToken("internal-token");
        properties.setLoanConnectTimeoutMs(0);
        properties.setLoanReadTimeoutMs(0);
        return properties;
    }

    @Test
    void getInstallment_parsesLoanServiceEnvelope() {
        PaymentProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LoanServiceClient client = new LoanServiceClient(properties, builder);
        UUID installmentId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();

        server.expect(requestTo("http://loan.test/api/v1/loans/internal/installments/" + installmentId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "internal-token"))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"installmentId":"%s","loanApplicationId":"%s",
                        "borrowerId":"%s","installmentNumber":1,"dueDate":"2026-11-24",
                        "amount":1375000.00,"status":"PENDING","paidAt":null}}
                        """.formatted(installmentId, applicationId, borrowerId), MediaType.APPLICATION_JSON));

        InstallmentInfo info = client.getInstallment(installmentId);

        assertThat(info.installmentId()).isEqualTo(installmentId);
        assertThat(info.loanApplicationId()).isEqualTo(applicationId);
        assertThat(info.borrowerId()).isEqualTo(borrowerId);
        assertThat(info.amount()).isEqualByComparingTo("1375000.00");
        assertThat(info.installmentStatus()).isEqualTo("PENDING");
        server.verify();
    }

    @Test
    void getLoanApplication_parsesAmountAndStatus() {
        PaymentProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LoanServiceClient client = new LoanServiceClient(properties, builder);
        UUID applicationId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();

        server.expect(requestTo("http://loan.test/api/v1/loans/internal/applications/" + applicationId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "internal-token"))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"loanApplicationId":"%s","borrowerId":"%s",
                        "amount":10000000.00,"status":"APPROVED"}}
                        """.formatted(applicationId, borrowerId), MediaType.APPLICATION_JSON));

        LoanApplicationInfo info = client.getLoanApplication(applicationId);

        assertThat(info.loanApplicationId()).isEqualTo(applicationId);
        assertThat(info.borrowerId()).isEqualTo(borrowerId);
        assertThat(info.amount()).isEqualByComparingTo("10000000.00");
        assertThat(info.status()).isEqualTo("APPROVED");
        server.verify();
    }

    @Test
    void markInstallmentPaid_postsPaidTimestamp() {
        PaymentProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LoanServiceClient client = new LoanServiceClient(properties, builder);
        UUID installmentId = UUID.randomUUID();

        server.expect(requestTo("http://loan.test/api/v1/loans/internal/installments/" + installmentId + "/paid"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Token", "internal-token"))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        client.markInstallmentPaid(installmentId, Instant.parse("2026-10-24T10:05:00Z"));

        server.verify();
    }
}
