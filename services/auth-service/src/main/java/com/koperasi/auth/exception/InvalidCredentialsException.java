package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Email atau kata sandi salah", HttpStatus.UNAUTHORIZED);
    }
}
