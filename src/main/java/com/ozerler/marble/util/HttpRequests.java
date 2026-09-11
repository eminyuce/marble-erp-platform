package com.ozerler.marble.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;

public final class HttpRequests {

    private static final String REQUESTED_WITH_HEADER = "X-Requested-With";
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    private HttpRequests() {
    }

    public static boolean expectsJson(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        return XML_HTTP_REQUEST.equals(request.getHeader(REQUESTED_WITH_HEADER));
    }
}
