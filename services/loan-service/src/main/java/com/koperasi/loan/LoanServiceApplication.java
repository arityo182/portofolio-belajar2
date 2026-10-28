package com.koperasi.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Core service: lending lifecycle plus the notification and reporting modules that used
 * to run as separate deployables. Module boundaries are kept by package, and reporting
 * still uses its own read-only datasource.
 */
@SpringBootApplication(scanBasePackages = {
        "com.koperasi.loan",
        "com.koperasi.notification",
        "com.koperasi.reporting"
})
@ConfigurationPropertiesScan({
        "com.koperasi.loan.config",
        "com.koperasi.notification.config"
})
@EnableJpaRepositories(basePackages = {
        "com.koperasi.loan.repository",
        "com.koperasi.notification.repository"
})
@EntityScan(basePackages = {
        "com.koperasi.loan.domain.entity",
        "com.koperasi.notification.domain.entity"
})
@EnableScheduling
public class LoanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
