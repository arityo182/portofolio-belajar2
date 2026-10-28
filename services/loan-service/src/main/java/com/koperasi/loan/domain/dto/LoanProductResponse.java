package com.koperasi.loan.domain.dto;

import com.koperasi.loan.domain.entity.LoanProduct;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record LoanProductResponse(
        Integer id,
        String name,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal interestRateAnnual,
        Integer minTenorMonths,
        Integer maxTenorMonths,
        @JsonProperty("isActive") boolean isActive
) {
    public static LoanProductResponse from(LoanProduct product) {
        return new LoanProductResponse(
                product.getId(),
                product.getName(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getInterestRateAnnual(),
                product.getMinTenorMonths(),
                product.getMaxTenorMonths(),
                product.isActive()
        );
    }
}
