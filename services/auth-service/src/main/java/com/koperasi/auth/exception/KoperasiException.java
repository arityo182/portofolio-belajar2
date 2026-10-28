package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public abstract class KoperasiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    protected KoperasiException(ErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    protected KoperasiException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return code;
    }

    public String getCode() {
        return code.name();
    }

    public HttpStatus getStatus() {
        return status;
    }
}
