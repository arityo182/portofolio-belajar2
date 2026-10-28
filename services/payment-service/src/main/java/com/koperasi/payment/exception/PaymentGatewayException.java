package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentGatewayException extends TechnicalException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(ErrorCode.PAYMENT_GATEWAY_ERROR, message, HttpStatus.BAD_GATEWAY, cause);
    }

    public PaymentGatewayException(String message) {
        super(ErrorCode.PAYMENT_GATEWAY_ERROR, message, HttpStatus.BAD_GATEWAY, null);
    }
}
