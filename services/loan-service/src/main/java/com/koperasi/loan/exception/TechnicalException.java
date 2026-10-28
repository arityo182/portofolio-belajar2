package com.koperasi.loan.exception;

import org.springframework.http.HttpStatus;

public abstract class TechnicalException extends KoperasiException {

    protected TechnicalException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
