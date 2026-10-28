package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS, "Email sudah terdaftar", HttpStatus.CONFLICT);
    }
}
