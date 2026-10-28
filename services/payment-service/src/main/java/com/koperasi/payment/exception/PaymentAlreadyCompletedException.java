package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentAlreadyCompletedException extends BusinessException {

    public PaymentAlreadyCompletedException() {
        super(ErrorCode.PAYMENT_ALREADY_COMPLETED, "Cicilan sudah dibayar", HttpStatus.CONFLICT);
    }
}
