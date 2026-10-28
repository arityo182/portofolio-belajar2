package com.koperasi.loan.exception;

import org.springframework.http.HttpStatus;

public class RejectionReasonRequiredException extends BusinessException {

    public RejectionReasonRequiredException() {
        super(ErrorCode.VALIDATION_ERROR,
                "Alasan penolakan wajib diisi ketika status REJECTED",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
