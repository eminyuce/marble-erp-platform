package com.ozerler.marble.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIps {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String REAL_IP_HEADER = "X-Real-IP";

    private ClientIps() {
    }

    public static String from(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
        if (isPresent(forwarded)) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader(REAL_IP_HEADER);
        if (isPresent(realIp)) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "" : remoteAddr;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
