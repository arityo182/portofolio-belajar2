package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.enums.LoanStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateLoanStatusRequest(
        @NotNull(message = "Status wajib diisi")
        LoanStatus status,

        @Size(max = 500, message = "Alasan penolakan maksimal 500 karakter")
        String rejectionReason,

        Boolean overrideScore,

        @Size(max = 1000, message = "Catatan review maksimal 1000 karakter")
        String notes,

        @NotNull(message = "Versi data wajib diisi")
        Long version
) {
}
