package com.koperasi.loan.service;

import com.koperasi.loan.domain.dto.LoanProductRequest;
import com.koperasi.loan.domain.dto.LoanProductResponse;
import com.koperasi.loan.domain.entity.LoanProduct;
import com.koperasi.loan.exception.BusinessValidationException;
import com.koperasi.loan.exception.LoanProductNotFoundException;
import com.koperasi.loan.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "loanProducts", key = "'active'")
    public List<LoanProductResponse> listActiveProducts() {
        return loanProductRepository.findByActiveTrueOrderByMinAmountAsc().stream()
                .map(LoanProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanProduct getActiveProduct(Integer productId) {
        return loanProductRepository.findById(productId)
                .filter(LoanProduct::isActive)
                .orElseThrow(() -> new LoanProductNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public List<LoanProductResponse> listAllProducts() {
        return loanProductRepository.findAll().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .map(LoanProductResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "loanProducts", allEntries = true)
    public LoanProductResponse create(LoanProductRequest request) {
        validate(request);
        LoanProduct product = new LoanProduct();
        apply(product, request);
        product.setActive(request.active() == null || request.active());
        return LoanProductResponse.from(loanProductRepository.saveAndFlush(product));
    }

    @Transactional
    @CacheEvict(cacheNames = "loanProducts", allEntries = true)
    public LoanProductResponse update(Integer productId, LoanProductRequest request) {
        validate(request);
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new LoanProductNotFoundException(productId));
        apply(product, request);
        if (request.active() != null) {
            product.setActive(request.active());
        }
        return LoanProductResponse.from(loanProductRepository.saveAndFlush(product));
    }

    @Transactional
    @CacheEvict(cacheNames = "loanProducts", allEntries = true)
    public void deactivate(Integer productId) {
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new LoanProductNotFoundException(productId));
        product.setActive(false);
        loanProductRepository.save(product);
    }

    private void apply(LoanProduct product, LoanProductRequest request) {
        product.setName(request.name().trim());
        product.setMinAmount(request.minAmount());
        product.setMaxAmount(request.maxAmount());
        product.setInterestRateAnnual(request.interestRateAnnual());
        product.setMinTenorMonths(request.minTenorMonths());
        product.setMaxTenorMonths(request.maxTenorMonths());
    }

    private void validate(LoanProductRequest request) {
        if (request.minAmount().compareTo(request.maxAmount()) > 0) {
            throw new BusinessValidationException("Nominal minimum tidak boleh melebihi nominal maksimum");
        }
        if (request.minTenorMonths() > request.maxTenorMonths()) {
            throw new BusinessValidationException("Tenor minimum tidak boleh melebihi tenor maksimum");
        }
        if (request.interestRateAnnual().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessValidationException("Suku bunga tahunan tidak boleh melebihi 100%");
        }
    }
}
