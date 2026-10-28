package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends KoperasiException {

    protected BusinessException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }
}
