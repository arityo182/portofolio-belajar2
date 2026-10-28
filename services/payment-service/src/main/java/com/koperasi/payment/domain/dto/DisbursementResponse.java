package com.koperasi.payment.domain.dto;

import com.koperasi.payment.domain.entity.LoanDisbursement;
import com.koperasi.payment.domain.enums.DisbursementStatus;
import com.koperasi.payment.domain.enums.PaymentGateway;

import java.math.BigDecimal;
import java.util.UUID;

public record DisbursementResponse(
        UUID disbursementId,
        UUID loanApplicationId,
        DisbursementStatus status,
        PaymentGateway paymentGateway,
        BigDecimal amount,
        String externalTransactionId
) {
    public static DisbursementResponse from(LoanDisbursement disbursement) {
        return new DisbursementResponse(
                disbursement.getId(),
                disbursement.getLoanApplicationId(),
                disbursement.getStatus(),
                disbursement.getPaymentGateway(),
                disbursement.getAmount(),
                disbursement.getExternalTransactionId()
        );
    }
}
