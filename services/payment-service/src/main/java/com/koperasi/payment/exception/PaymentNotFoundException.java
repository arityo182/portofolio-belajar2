package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException(Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Pembayaran tidak ditemukan: " + id, HttpStatus.NOT_FOUND);
    }
}
