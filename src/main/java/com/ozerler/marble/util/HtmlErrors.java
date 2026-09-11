package com.ozerler.marble.util;

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

    private static final String NO_MESSAGE_AVAILABLE = "No message available";
    private static final String UNKNOWN_PATH = "/";
    private static final String DEFAULT_METHOD = "GET";
    private static final String TONE_CLIENT = "client";
    private static final String TONE_FORBIDDEN = "forbidden";
    private static final String TONE_MISSING = "missing";
    private static final String TONE_SERVER = "server";

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
        return switch (status) {
            case 400 -> "Geçersiz istek";
            case 401 -> "Oturum gerekli";
            case 403 -> "Erişim engellendi";
            case 404 -> "Sayfa bulunamadı";
            case 405 -> "Yöntem desteklenmiyor";
            case 409 -> "Çakışma";
            case 429 -> "Çok fazla istek";
            case 500 -> "Sunucu hatası";
            case 502, 503, 504 -> "Servis kullanılamıyor";
            default -> "İstek başarısız";
        };
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
            return TONE_FORBIDDEN;
        }
        if (status == 404) {
            return TONE_MISSING;
        }
        if (status >= 500) {
            return TONE_SERVER;
        }
        return TONE_CLIENT;
    }

    public static String messageFor(int status, String serverMessage) {
        if (isUsableMessage(serverMessage)) {
            return serverMessage;
        }
        return switch (status) {
            case 400 -> "Gönderilen istek geçersiz veya eksik.";
            case 401 -> "Bu sayfayı görmek için oturum açmanız gerekir.";
            case 403 -> "Bu işlem için yetkiniz yok.";
            case 404 -> "İstenen sayfa veya kaynak bulunamadı.";
            case 405 -> "Bu adres için kullanılan HTTP yöntemi desteklenmiyor.";
            case 429 -> "Kısa süre içinde çok fazla istek gönderildi. Biraz bekleyip tekrar deneyin.";
            case 500 -> "Beklenmeyen bir sunucu hatası oluştu. Ayrıntılar aşağıda.";
            case 502, 503, 504 -> "Bağımlı servis şu anda yanıt vermiyor.";
            default -> "İstek tamamlanamadı. Aşağıdaki ayrıntılar hatayı düzeltmeye yardımcı olur.";
        };
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
            return UNKNOWN_PATH;
        }
        String requestUri = request.getRequestURI();
        return Strings.isPresent(requestUri) ? requestUri : UNKNOWN_PATH;
    }

    public static String methodOf(HttpServletRequest request) {
        if (request == null || !Strings.isPresent(request.getMethod())) {
            return DEFAULT_METHOD;
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
        return Strings.isPresent(serverMessage) && !NO_MESSAGE_AVAILABLE.equalsIgnoreCase(serverMessage.trim());
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
