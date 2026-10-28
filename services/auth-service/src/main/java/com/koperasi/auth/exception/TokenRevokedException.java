package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class TokenRevokedException extends BusinessException {

    public TokenRevokedException(String message) {
        super(ErrorCode.TOKEN_REVOKED, message, HttpStatus.UNAUTHORIZED);
    }

    public TokenRevokedException() {
        this("Token telah dicabut, silakan login kembali");
    }
}
