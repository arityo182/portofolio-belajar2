package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class DistributedLockException extends BusinessException {

    public DistributedLockException(String message) {
        super(ErrorCode.IDEMPOTENCY_CONFLICT, message, HttpStatus.CONFLICT);
    }
}
