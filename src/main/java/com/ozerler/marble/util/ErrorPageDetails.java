package com.ozerler.marble.util;

public record ErrorPageDetails(
        int status,
        String reason,
        String title,
        String icon,
        String tone,
        String message,
        String path,
        String method,
        String queryString,
        String timestamp,
        String exceptionType,
        String stackTrace
) {

    public boolean hasStackTrace() {
        return Strings.isPresent(stackTrace);
    }

    public boolean hasQueryString() {
        return Strings.isPresent(queryString);
    }

    public boolean hasExceptionType() {
        return Strings.isPresent(exceptionType);
    }

    public String retryHref() {
        if (!Strings.isPresent(path) || "/error".equals(path)) {
            return "/admin/dashboard";
        }
        if (!hasQueryString()) {
            return path;
        }
        return path + "?" + queryString;
    }
}
