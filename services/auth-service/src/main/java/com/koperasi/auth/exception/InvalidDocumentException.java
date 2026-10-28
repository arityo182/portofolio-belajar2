package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidDocumentException extends BusinessException {

    public InvalidDocumentException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST);
    }
}
