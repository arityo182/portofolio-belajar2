package com.koperasi.auth.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUtilsTest {

    @Test
    void clientIp_prefersRealIpOverForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.7");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        assertThat(RequestUtils.clientIp(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void clientIp_usesFirstForwardedForEntryWhenRealIpMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        assertThat(RequestUtils.clientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void clientIp_usesRealIpHeaderWhenForwardedForMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.7");

        assertThat(RequestUtils.clientIp(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void clientIp_fallsBackToRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        assertThat(RequestUtils.clientIp(request)).isEqualTo("10.0.0.1");
        assertThat(RequestUtils.userAgent(request)).isEqualTo("unknown");
    }

    @Test
    void deviceInfo_combinesIpAndUserAgentAndTruncates() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("User-Agent", "J".repeat(800));

        String deviceInfo = RequestUtils.deviceInfo(request);

        assertThat(deviceInfo).startsWith("10.0.0.1 | ").hasSize(500);
    }
}
