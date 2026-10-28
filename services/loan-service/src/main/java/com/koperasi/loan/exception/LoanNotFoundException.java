package com.koperasi.loan.exception;

import org.springframework.http.HttpStatus;

public class LoanNotFoundException extends BusinessException {

    public LoanNotFoundException(Object loanId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Pinjaman tidak ditemukan: " + loanId, HttpStatus.NOT_FOUND);
    }
}
