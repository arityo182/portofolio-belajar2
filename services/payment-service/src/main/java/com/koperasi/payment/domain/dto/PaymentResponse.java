package com.koperasi.payment.domain.dto;

import com.koperasi.payment.domain.entity.Payment;
import com.koperasi.payment.domain.enums.PaymentGateway;
import com.koperasi.payment.domain.enums.PaymentStatus;

import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID installmentId,
        PaymentStatus status,
        PaymentGateway paymentGateway,
        String paymentUrl,
        String externalTransactionId
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getLoanInstallmentId(),
                payment.getStatus(),
                payment.getPaymentGateway(),
                payment.getPaymentUrl(),
                payment.getExternalTransactionId()
        );
    }
}
