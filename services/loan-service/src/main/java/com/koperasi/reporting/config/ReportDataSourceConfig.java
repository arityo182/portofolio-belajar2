package com.koperasi.reporting.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Reporting keeps least privilege even inside the core service: queries run through a
 * separate read-only datasource owned by report_user, never through loan_user.
 */
@Configuration
public class ReportDataSourceConfig {

    @Bean(name = "reportDataSource")
    public DataSource reportDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(env("REPORT_DB_URL",
                        "jdbc:postgresql://localhost:5432/koperasi?currentSchema=public"))
                .username(env("REPORT_DB_USERNAME", "report_user"))
                .password(env("REPORT_DB_PASSWORD", "reportP@ss"))
                .driverClassName("org.postgresql.Driver")
                .build();
        dataSource.setPoolName("report-read-only-pool");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(0);
        dataSource.setReadOnly(true);
        // Reporting is optional for most requests; never block application startup when
        // the read-only pool is temporarily unreachable (tests, local runs without data).
        dataSource.setInitializationFailTimeout(-1);
        return dataSource;
    }

    @Bean(name = "reportJdbcTemplate")
    public NamedParameterJdbcTemplate reportJdbcTemplate(
            @Qualifier("reportDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
