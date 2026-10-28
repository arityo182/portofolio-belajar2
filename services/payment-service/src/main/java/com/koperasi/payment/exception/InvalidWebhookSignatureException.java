package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class InvalidWebhookSignatureException extends BusinessException {

    public InvalidWebhookSignatureException(String message) {
        super(ErrorCode.INVALID_WEBHOOK_SIGNATURE, message, HttpStatus.BAD_REQUEST);
    }
}
