package com.koperasi.loan.exception;

import org.springframework.http.HttpStatus;

public class LoanProductNotFoundException extends BusinessException {

    public LoanProductNotFoundException(Object productId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Produk pinjaman tidak ditemukan atau tidak aktif: " + productId,
                HttpStatus.NOT_FOUND);
    }
}
