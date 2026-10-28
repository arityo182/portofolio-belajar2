package com.koperasi.loan.exception;

import org.springframework.http.HttpStatus;

public class ConcurrentModificationException extends BusinessException {

    private final String detail;

    public ConcurrentModificationException(Object expectedVersion, Object actualVersion) {
        super(ErrorCode.CONCURRENT_MODIFICATION,
                "Data telah diubah oleh proses lain, muat ulang",
                HttpStatus.CONFLICT);
        this.detail = "expectedVersion=" + expectedVersion + ", actualVersion=" + actualVersion;
    }

    public String getDetail() {
        return detail;
    }
}
