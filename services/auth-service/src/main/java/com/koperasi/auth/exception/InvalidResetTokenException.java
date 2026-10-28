package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidResetTokenException extends BusinessException {

    public InvalidResetTokenException(String message) {
        super(ErrorCode.INVALID_RESET_TOKEN, message, HttpStatus.BAD_REQUEST);
    }
}
