package com.koperasi.loan.exception;

import org.springframework.http.HttpStatus;

public class ScoreNotFoundException extends BusinessException {

    public ScoreNotFoundException(Object loanId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Skor kredit belum tersedia untuk pinjaman: " + loanId,
                HttpStatus.NOT_FOUND);
    }
}
