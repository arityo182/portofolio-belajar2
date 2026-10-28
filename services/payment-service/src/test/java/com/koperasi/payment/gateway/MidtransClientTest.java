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

class MidtransClientTest {

    @Test
    void createPaymentLink_mapsMidtransResponse() {
        PaymentProperties properties = new PaymentProperties();
        properties.getMidtrans().setServerKey("server-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MidtransClient client = new MidtransClient(properties, builder);
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1375000.00"), PaymentGateway.MIDTRANS, "idem-1");

        server.expect(requestTo("https://app.sandbox.midtrans.com/snap/v1/transactions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"redirect_url\":\"https://pay.test/1\",\"token\":\"MID-TOKEN\"}",
                        MediaType.APPLICATION_JSON));

        PaymentLinkResult result = client.createPaymentLink(payment);

        assertThat(result.paymentUrl()).isEqualTo("https://pay.test/1");
        assertThat(result.externalTransactionId()).isEqualTo("MID-TOKEN");
        server.verify();
    }

    @Test
    void initiateDisbursement_mapsMidtransResponse() {
        PaymentProperties properties = new PaymentProperties();
        properties.getMidtrans().setServerKey("server-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MidtransClient client = new MidtransClient(properties, builder);
        LoanDisbursement disbursement = LoanDisbursement.create(UUID.randomUUID(),
                new BigDecimal("10000000.00"), PaymentGateway.MIDTRANS, "disb-1");

        server.expect(requestTo("https://api.sandbox.midtrans.com/v1/payouts"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"reference_no\":\"MID-REF-1\",\"status\":\"PENDING\"}",
                        MediaType.APPLICATION_JSON));

        DisbursementResult result = client.initiateDisbursement(disbursement);

        assertThat(result.externalTransactionId()).isEqualTo("MID-REF-1");
        assertThat(result.status()).isEqualTo("PENDING");
        server.verify();
    }

    @Test
    void createPaymentLink_withoutServerKey_throwsGatewayException() {
        PaymentProperties properties = new PaymentProperties();
        MidtransClient client = new MidtransClient(properties, RestClient.builder());
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), PaymentGateway.MIDTRANS, "idem-1");

        assertThatThrownBy(() -> client.createPaymentLink(payment))
                .isInstanceOf(PaymentGatewayException.class);
    }
}
