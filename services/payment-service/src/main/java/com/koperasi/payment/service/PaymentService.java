package com.koperasi.payment.service;

import com.koperasi.payment.domain.dto.CreateDisbursementRequest;
import com.koperasi.payment.domain.dto.CreatePaymentRequest;
import com.koperasi.payment.domain.dto.DisbursementResponse;
import com.koperasi.payment.domain.dto.PaymentResponse;
import com.koperasi.payment.domain.dto.PaymentStatusResponse;
import com.koperasi.payment.security.JwtPrincipal;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse payInstallment(UUID installmentId, CreatePaymentRequest request,
                                   String idempotencyKey, JwtPrincipal principal);

    PaymentStatusResponse getInstallmentStatus(UUID installmentId, JwtPrincipal principal);

    DisbursementResponse initiateDisbursement(CreateDisbursementRequest request,
                                              String idempotencyKey, JwtPrincipal principal);

    void retryPayment(UUID paymentId);
}
