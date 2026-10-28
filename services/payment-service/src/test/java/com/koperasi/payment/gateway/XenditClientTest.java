package com.koperasi.payment.gateway;

import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.domain.enums.PaymentGateway;
import com.koperasi.payment.exception.PaymentGatewayException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class XenditClientTest {

    @Test
    void createPaymentLink_mapsInvoiceResponse() {
        PaymentProperties properties = new PaymentProperties();
        properties.getXendit().setApiKey("xnd-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        XenditClient client = new XenditClient(properties, builder);
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1375000.00"), PaymentGateway.XENDIT, "idem-1");

        server.expect(requestTo("https://api.xendit.co/v2/invoices"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"id\":\"XEN-INV-1\",\"invoice_url\":\"https://xendit.test/1\"}",
                        MediaType.APPLICATION_JSON));

        PaymentLinkResult result = client.createPaymentLink(payment);

        assertThat(result.paymentUrl()).isEqualTo("https://xendit.test/1");
        assertThat(result.externalTransactionId()).isEqualTo("XEN-INV-1");
        server.verify();
    }

    @Test
    void initiateDisbursement_mapsXenditResponse() {
        PaymentProperties properties = new PaymentProperties();
        properties.getXendit().setApiKey("xnd-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        XenditClient client = new XenditClient(properties, builder);
        LoanDisbursement disbursement = LoanDisbursement.create(UUID.randomUUID(),
                new BigDecimal("10000000.00"), PaymentGateway.XENDIT, "disb-1");

        server.expect(requestTo("https://api.xendit.co/disbursements"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"id\":\"XEN-DISB-1\",\"status\":\"PENDING\"}",
                        MediaType.APPLICATION_JSON));

        DisbursementResult result = client.initiateDisbursement(disbursement);

        assertThat(result.externalTransactionId()).isEqualTo("XEN-DISB-1");
        assertThat(result.status()).isEqualTo("PENDING");
        server.verify();
    }

    @Test
    void createPaymentLink_withoutApiKey_throwsGatewayException() {
        PaymentProperties properties = new PaymentProperties();
        XenditClient client = new XenditClient(properties, RestClient.builder());
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), PaymentGateway.XENDIT, "idem-1");

        assertThatThrownBy(() -> client.createPaymentLink(payment))
                .isInstanceOf(PaymentGatewayException.class);
    }
}
