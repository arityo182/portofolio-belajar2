package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountSuspendedException extends BusinessException {

    public AccountSuspendedException() {
        super(ErrorCode.ACCOUNT_SUSPENDED, "Akun ditangguhkan, hubungi administrator", HttpStatus.FORBIDDEN);
    }
}
