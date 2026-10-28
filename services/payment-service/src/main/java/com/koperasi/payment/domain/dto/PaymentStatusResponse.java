package com.koperasi.payment.domain.dto;

import com.koperasi.payment.domain.entity.Payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentStatusResponse(
        UUID installmentId,
        String installmentStatus,
        String paymentStatus,
        Instant paidAt,
        String externalTransactionId
) {
    public static PaymentStatusResponse from(Payment payment) {
        String installmentStatus = payment.getStatus() == com.koperasi.payment.domain.enums.PaymentStatus.SUCCESS
                ? "PAID"
                : "PENDING";
        return new PaymentStatusResponse(
                payment.getLoanInstallmentId(),
                installmentStatus,
                payment.getStatus().name(),
                payment.getPaidAt(),
                payment.getExternalTransactionId()
        );
    }
}
