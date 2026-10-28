package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class RoleNotAllowedException extends BusinessException {

    public RoleNotAllowedException(String message) {
        super(ErrorCode.ROLE_NOT_ALLOWED, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
