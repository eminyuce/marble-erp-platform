package com.ozerler.marble.util;

import jakarta.servlet.http.HttpServletRequest;

import com.ozerler.marble.common.Constants;

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
        if (remoteAddr == null) {
            return "";
        }
        return remoteAddr;
    }

    /**
     * Address resolved by the container's RemoteIpValve. Forwarding headers are honoured only when the
     * request came from a configured internal proxy, so a client cannot pick its own address by sending
     * X-Forwarded-For. Use this for security decisions such as login throttling.
     *
     * @param request request
     * @return the trusted client address, never null
     */
    public static String trusted(HttpServletRequest request) {
        if (request == null || request.getRemoteAddr() == null) {
            return "";
        }
        return request.getRemoteAddr();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
