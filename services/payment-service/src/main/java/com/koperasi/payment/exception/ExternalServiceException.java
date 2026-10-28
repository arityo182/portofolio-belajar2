package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends TechnicalException {

    public ExternalServiceException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }

    public ExternalServiceException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message, HttpStatus.SERVICE_UNAVAILABLE, null);
    }
}
