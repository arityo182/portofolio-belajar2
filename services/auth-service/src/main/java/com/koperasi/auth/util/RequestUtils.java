package com.koperasi.auth.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {

    private static final int MAX_DEVICE_INFO_LENGTH = 500;

    private RequestUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String userAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "unknown" : userAgent;
    }

    public static String deviceInfo(HttpServletRequest request) {
        String value = clientIp(request) + " | " + userAgent(request);
        return value.length() > MAX_DEVICE_INFO_LENGTH ? value.substring(0, MAX_DEVICE_INFO_LENGTH) : value;
    }
}
