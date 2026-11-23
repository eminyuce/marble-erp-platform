package com.ozerler.marble.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import com.ozerler.marble.common.Constants;

public final class HtmlErrors {

    private static final int BAD_GATEWAY = 502;
    private static final int BAD_REQUEST = 400;
    private static final int CONFLICT = 409;
    private static final int FORBIDDEN = 403;
    private static final int GATEWAY_TIMEOUT = 504;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int METHOD_NOT_ALLOWED = 405;
    private static final int NOT_FOUND = 404;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int UNAUTHORIZED = 401;

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

        String reasonValue;
        if (Strings.isPresent(reason)) {
            reasonValue = reason;
        } else {
            reasonValue = defaultReason(status);
        }
        Object formatTimestampValue;
        if (attributes == null) {
            formatTimestampValue = null;
        } else {
            formatTimestampValue = attributes.get("timestamp");
        }
        return new ErrorPageDetails(
                status,
                reasonValue,
                titleFor(status),
                iconFor(status),
                toneFor(status),
                messageFor(status, serverMessage),
                pathOf(request, attributePath),
                methodOf(request),
                queryStringOf(request),
                formatTimestamp(formatTimestampValue),
                exceptionTypeOf(error, attributeException),
                stackTraceOf(error, attributeTrace)
        );
    }

    public static String titleFor(int status) {
        String key = switch (status) {
            case BAD_REQUEST -> "error.http.400.title";
            case UNAUTHORIZED -> "error.http.401.title";
            case FORBIDDEN -> "error.http.403.title";
            case NOT_FOUND -> "error.http.404.title";
            case METHOD_NOT_ALLOWED -> "error.http.405.title";
            case CONFLICT -> "error.http.409.title";
            case TOO_MANY_REQUESTS -> "error.http.429.title";
            case INTERNAL_SERVER_ERROR -> "error.http.500.title";
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> "error.http.502.title";
            default -> "error.http.default.title";
        };
        return MessageUtils.getMessage(key);
    }

    public static String iconFor(int status) {
        return switch (status) {
            case BAD_REQUEST -> "circle-alert";
            case UNAUTHORIZED -> "log-in";
            case FORBIDDEN -> "shield-off";
            case NOT_FOUND -> "search-x";
            case METHOD_NOT_ALLOWED -> "ban";
            case TOO_MANY_REQUESTS -> "timer-off";
            case INTERNAL_SERVER_ERROR -> "server-crash";
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> "unplug";
            default -> "triangle-alert";
        };
    }

    public static String toneFor(int status) {
        if (status == FORBIDDEN) {
            return Constants.ERROR_TONE_FORBIDDEN;
        }
        if (status == NOT_FOUND) {
            return Constants.ERROR_TONE_MISSING;
        }
        if (status >= INTERNAL_SERVER_ERROR) {
            return Constants.ERROR_TONE_SERVER;
        }
        return Constants.ERROR_TONE_CLIENT;
    }

    public static String messageFor(int status, String serverMessage) {
        if (isUsableMessage(serverMessage)) {
            return serverMessage;
        }
        String key = switch (status) {
            case BAD_REQUEST -> "error.http.400.message";
            case UNAUTHORIZED -> "error.http.401.message";
            case FORBIDDEN -> "error.http.403.message";
            case NOT_FOUND -> "error.http.404.message";
            case METHOD_NOT_ALLOWED -> "error.http.405.message";
            case CONFLICT -> "error.http.409.message";
            case TOO_MANY_REQUESTS -> "error.http.429.message";
            case INTERNAL_SERVER_ERROR -> "error.http.500.message";
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> "error.http.502.message";
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
        if (Strings.isPresent(requestUri)) {
            return requestUri;
        }
        return Constants.ERROR_UNKNOWN_PATH;
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
        if (forwarded == null) {
            return "";
        }
        return forwarded;
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
        if (attributeException == null) {
            return "";
        }
        return attributeException;
    }

    public static String stackTraceOf(Throwable error, String attributeTrace) {
        if (error != null) {
            StringWriter writer = new StringWriter();
            error.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
        if (attributeTrace == null) {
            return "";
        }
        return attributeTrace;
    }

    public static String defaultReason(int status) {
        return switch (status) {
            case BAD_REQUEST -> "Bad Request";
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Not Found";
            case METHOD_NOT_ALLOWED -> "Method Not Allowed";
            case CONFLICT -> "Conflict";
            case TOO_MANY_REQUESTS -> "Too Many Requests";
            case INTERNAL_SERVER_ERROR -> "Internal Server Error";
            case BAD_GATEWAY -> "Bad Gateway";
            case SERVICE_UNAVAILABLE -> "Service Unavailable";
            case GATEWAY_TIMEOUT -> "Gateway Timeout";
            default -> "Error";
        };
    }

    private static boolean isUsableMessage(String serverMessage) {
        return Strings.isPresent(serverMessage)
                && !Constants.ERROR_NO_MESSAGE_AVAILABLE.equalsIgnoreCase(serverMessage.trim());
    }

    private static String stringValue(Map<String, ?> attributes, String key) {
        if (attributes == null || key == null) {
            return "";
        }
        Object value = attributes.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private static String attribute(HttpServletRequest request, String name) {
        if (request == null) {
            return "";
        }
        Object value = request.getAttribute(name);
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }
}
