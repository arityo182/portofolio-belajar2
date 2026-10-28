package com.koperasi.auth.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_withoutCorrelationHeader_generatesTraceIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();
        AtomicReference<String> serviceId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            traceId.set(MDC.get("traceId"));
            serviceId.set(MDC.get("serviceId"));
        });

        assertThat(traceId.get()).isNotBlank().isEqualTo(response.getHeader("X-Correlation-Id"));
        assertThat(serviceId.get()).isEqualTo("auth-service");
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("serviceId")).isNull();
    }

    @Test
    void doFilter_withCorrelationHeader_reusesIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("X-Correlation-Id", "existing-trace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> traceId.set(MDC.get("traceId")));

        assertThat(traceId.get()).isEqualTo("existing-trace");
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("existing-trace");
    }
}
