package com.koperasi.payment.gateway;

public record PaymentLinkResult(String paymentUrl, String externalTransactionId) {
}
