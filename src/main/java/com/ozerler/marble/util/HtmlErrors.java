package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Map;

public final class HtmlErrors {

    private HtmlErrors() {
    }

    public static ErrorPageDetails page(
            int status,
            String reason,
            Map<String, ?> attributes,
            HttpServletRequest request,
            Throwable error
    ) {
        String serverMessage = stringValue(attributes, "message");
        String attributePath = stringValue(attributes, "path");
        String attributeException = stringValue(attributes, "exception");
        String attributeTrace = stringValue(attributes, "trace");

        return new ErrorPageDetails(
                status,
                Strings.isPresent(reason) ? reason : defaultReason(status),
                titleFor(status),
                iconFor(status),
                toneFor(status),
                messageFor(status, serverMessage),
                pathOf(request, attributePath),
                methodOf(request),
                queryStringOf(request),
                formatTimestamp(attributes == null ? null : attributes.get("timestamp")),
                exceptionTypeOf(error, attributeException),
                stackTraceOf(error, attributeTrace)
        );
    }

    public static String titleFor(int status) {
        String key = switch (status) {
            case 400 -> "error.http.400.title";
            case 401 -> "error.http.401.title";
            case 403 -> "error.http.403.title";
            case 404 -> "error.http.404.title";
            case 405 -> "error.http.405.title";
            case 409 -> "error.http.409.title";
            case 429 -> "error.http.429.title";
            case 500 -> "error.http.500.title";
            case 502, 503, 504 -> "error.http.502.title";
            default -> "error.http.default.title";
        };
        return MessageUtils.getMessage(key);
    }

    public static String iconFor(int status) {
        return switch (status) {
            case 400 -> "circle-alert";
            case 401 -> "log-in";
            case 403 -> "shield-off";
            case 404 -> "search-x";
            case 405 -> "ban";
            case 429 -> "timer-off";
            case 500 -> "server-crash";
            case 502, 503, 504 -> "unplug";
            default -> "triangle-alert";
        };
    }

    public static String toneFor(int status) {
        if (status == 403) {
            return Constants.ERROR_TONE_FORBIDDEN;
        }
        if (status == 404) {
            return Constants.ERROR_TONE_MISSING;
        }
        if (status >= 500) {
            return Constants.ERROR_TONE_SERVER;
        }
        return Constants.ERROR_TONE_CLIENT;
    }

    public static String messageFor(int status, String serverMessage) {
        if (isUsableMessage(serverMessage)) {
            return serverMessage;
        }
        String key = switch (status) {
            case 400 -> "error.http.400.message";
            case 401 -> "error.http.401.message";
            case 403 -> "error.http.403.message";
            case 404 -> "error.http.404.message";
            case 405 -> "error.http.405.message";
            case 409 -> "error.http.409.message";
            case 429 -> "error.http.429.message";
            case 500 -> "error.http.500.message";
            case 502, 503, 504 -> "error.http.502.message";
            default -> "error.http.default.message";
        };
        return MessageUtils.getMessage(key);
    }

    public static String pathOf(HttpServletRequest request, String attributePath) {
        String errorUri = attribute(request, RequestDispatcher.ERROR_REQUEST_URI);
        if (Strings.isPresent(errorUri) && !"/error".equals(errorUri)) {
            return errorUri;
        }
        String forwardedUri = attribute(request, RequestDispatcher.FORWARD_REQUEST_URI);
        if (Strings.isPresent(forwardedUri) && !"/error".equals(forwardedUri)) {
            return forwardedUri;
        }
        if (Strings.isPresent(attributePath) && !"/error".equals(attributePath)) {
            return attributePath;
        }
        if (request == null) {
            return Constants.ERROR_UNKNOWN_PATH;
        }
        String requestUri = request.getRequestURI();
        return Strings.isPresent(requestUri) ? requestUri : Constants.ERROR_UNKNOWN_PATH;
    }

    public static String methodOf(HttpServletRequest request) {
        if (request == null || !Strings.isPresent(request.getMethod())) {
            return Constants.DEFAULT_HTTP_METHOD;
        }
        return request.getMethod();
    }

    public static String queryStringOf(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        if (Strings.isPresent(request.getQueryString())) {
            return request.getQueryString();
        }
        String forwarded = attribute(request, RequestDispatcher.FORWARD_QUERY_STRING);
        return forwarded == null ? "" : forwarded;
    }

    public static String formatTimestamp(Object timestamp) {
        if (timestamp == null) {
            return DateTimes.formatYearMonthDayHourMinute(LocalDateTime.now(), "");
        }
        if (timestamp instanceof LocalDateTime localDateTime) {
            return DateTimes.formatYearMonthDayHourMinute(localDateTime, "");
        }
        if (timestamp instanceof TemporalAccessor temporalAccessor) {
            return DateTimes.formatYearMonthDayHourMinute(temporalAccessor, timestamp.toString());
        }
        if (timestamp instanceof Date date) {
            return DateTimes.formatYearMonthDayHourMinute(
                    LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()), "");
        }
        if (timestamp instanceof Instant instant) {
            return DateTimes.formatYearMonthDayHourMinute(
                    LocalDateTime.ofInstant(instant, ZoneId.systemDefault()), "");
        }
        return timestamp.toString();
    }

    public static String exceptionTypeOf(Throwable error, String attributeException) {
        if (error != null) {
            return error.getClass().getName();
        }
        return attributeException == null ? "" : attributeException;
    }

    public static String stackTraceOf(Throwable error, String attributeTrace) {
        if (error != null) {
            StringWriter writer = new StringWriter();
            error.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
        return attributeTrace == null ? "" : attributeTrace;
    }

    public static String defaultReason(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Error";
        };
    }

    private static boolean isUsableMessage(String serverMessage) {
        return Strings.isPresent(serverMessage) && !Constants.ERROR_NO_MESSAGE_AVAILABLE.equalsIgnoreCase(serverMessage.trim());
    }

    private static String stringValue(Map<String, ?> attributes, String key) {
        if (attributes == null || key == null) {
            return "";
        }
        Object value = attributes.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String attribute(HttpServletRequest request, String name) {
        if (request == null) {
            return "";
        }
        Object value = request.getAttribute(name);
        return value == null ? "" : String.valueOf(value);
    }
}
