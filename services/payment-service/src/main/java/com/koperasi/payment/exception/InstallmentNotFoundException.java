package com.koperasi.payment.exception;

import org.springframework.http.HttpStatus;

public class InstallmentNotFoundException extends BusinessException {

    public InstallmentNotFoundException(Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Cicilan tidak ditemukan: " + id, HttpStatus.NOT_FOUND);
    }
}
