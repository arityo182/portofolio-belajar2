package com.koperasi.loan.exception;

import com.koperasi.loan.domain.enums.LoanStatus;
import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(LoanStatus from, LoanStatus to) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                "Transisi status tidak diizinkan: " + from + " -> " + to, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public InvalidStateTransitionException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
