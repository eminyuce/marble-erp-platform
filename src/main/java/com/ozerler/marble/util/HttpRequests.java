package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;

public final class HttpRequests {

    private static final String REQUESTED_WITH_HEADER = Constants.REQUESTED_WITH_HEADER;
    private static final String XML_HTTP_REQUEST = Constants.XML_HTTP_REQUEST;

    private HttpRequests() {
    }

    public static boolean expectsJson(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader(Constants.HEADER_ACCEPT);
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        return XML_HTTP_REQUEST.equals(request.getHeader(REQUESTED_WITH_HEADER));
    }

    public static boolean isHtmx(HttpServletRequest request) {
        return request != null && "true".equalsIgnoreCase(request.getHeader(Constants.HEADER_HX_REQUEST));
    }
}
