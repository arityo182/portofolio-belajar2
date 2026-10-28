package com.koperasi.loan.domain.dto;

import java.time.Instant;

public record MarkInstallmentPaidRequest(Instant paidAt) {
}
