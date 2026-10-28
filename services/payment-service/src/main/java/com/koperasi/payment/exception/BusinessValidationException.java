package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class BusinessValidationException extends BusinessException {

    public BusinessValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
