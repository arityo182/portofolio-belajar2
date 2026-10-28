package com.koperasi.loan.exception;

import org.springframework.http.HttpStatus;

public class InstallmentNotFoundException extends BusinessException {

    public InstallmentNotFoundException(Object installmentId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Cicilan tidak ditemukan: " + installmentId, HttpStatus.NOT_FOUND);
    }
}
