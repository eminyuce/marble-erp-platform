package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;
import jakarta.servlet.http.HttpServletRequest;

public final class ClientIps {

    private ClientIps() {
    }

    public static String from(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String forwarded = request.getHeader(Constants.FORWARDED_FOR_HEADER);
        if (isPresent(forwarded)) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader(Constants.REAL_IP_HEADER);
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
