package com.koperasi.payment.gateway;

public record DisbursementResult(String externalTransactionId, String status) {
}
