package com.koperasi.loan.config;

import com.koperasi.loan.domain.enums.InstallmentMethod;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.loan")
public class LoanProperties {

    private long lockTtlSeconds = 10;
    private int maxPageSize = 100;
    private InstallmentMethod installmentMethod = InstallmentMethod.FLAT;
    private int defaultOverdueDays = 90;
    private String overdueCron = "0 5 0 * * *";
    private String schedulerZone = "Asia/Jakarta";
    private int overdueBatchSize = 500;
}
