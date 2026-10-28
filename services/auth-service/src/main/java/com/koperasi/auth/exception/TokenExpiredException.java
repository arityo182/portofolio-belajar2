package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends BusinessException {

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message, HttpStatus.UNAUTHORIZED);
    }

    public TokenExpiredException() {
        this("Token telah kedaluwarsa");
    }
}
