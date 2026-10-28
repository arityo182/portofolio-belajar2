package com.koperasi.loan.config;

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
    void doFilter_withoutHeader_generatesTraceIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();
        AtomicReference<String> serviceId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            traceId.set(MDC.get("traceId"));
            serviceId.set(MDC.get("serviceId"));
        });

        assertThat(traceId.get()).isNotBlank().isEqualTo(response.getHeader("X-Correlation-Id"));
        assertThat(serviceId.get()).isEqualTo("loan-service");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void doFilter_withHeader_reusesTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> traceId.set(MDC.get("traceId")));

        assertThat(traceId.get()).isEqualTo("trace-123");
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("trace-123");
    }
}
