package com.koperasi.loan.repository;

import com.koperasi.loan.domain.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Integer> {

    List<LoanProduct> findByActiveTrueOrderByMinAmountAsc();
}
