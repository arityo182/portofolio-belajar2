package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException(String message) {
        super(ErrorCode.IDEMPOTENCY_CONFLICT, message, HttpStatus.CONFLICT);
    }
}
