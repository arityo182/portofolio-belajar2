package com.koperasi.loan.service;

import com.koperasi.loan.domain.dto.LoanProductResponse;
import com.koperasi.loan.domain.entity.LoanProduct;
import com.koperasi.loan.exception.LoanProductNotFoundException;
import com.koperasi.loan.repository.LoanProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductRepository loanProductRepository;

    @InjectMocks
    private LoanProductService loanProductService;

    @Test
    void listActiveProducts_mapsRepositoryResults() {
        LoanProduct product = product(true);
        when(loanProductRepository.findByActiveTrueOrderByMinAmountAsc()).thenReturn(List.of(product));

        List<LoanProductResponse> products = loanProductService.listActiveProducts();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).id()).isEqualTo(1);
        assertThat(products.get(0).isActive()).isTrue();
    }

    @Test
    void getActiveProduct_whenActive_returnsProduct() {
        LoanProduct product = product(true);
        when(loanProductRepository.findById(1)).thenReturn(Optional.of(product));

        assertThat(loanProductService.getActiveProduct(1)).isSameAs(product);
    }

    @Test
    void getActiveProduct_whenInactiveOrMissing_throwsNotFound() {
        when(loanProductRepository.findById(1)).thenReturn(Optional.of(product(false)));
        when(loanProductRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanProductService.getActiveProduct(1))
                .isInstanceOf(LoanProductNotFoundException.class);
        assertThatThrownBy(() -> loanProductService.getActiveProduct(2))
                .isInstanceOf(LoanProductNotFoundException.class);
    }

    private LoanProduct product(boolean active) {
        LoanProduct product = new LoanProduct();
        product.setId(1);
        product.setName("Modal Usaha Mikro");
        product.setMinAmount(new BigDecimal("1000000.00"));
        product.setMaxAmount(new BigDecimal("50000000.00"));
        product.setInterestRateAnnual(new BigDecimal("18.00"));
        product.setMinTenorMonths(3);
        product.setMaxTenorMonths(24);
        product.setActive(active);
        product.setCreatedAt(Instant.now());
        return product;
    }
}
