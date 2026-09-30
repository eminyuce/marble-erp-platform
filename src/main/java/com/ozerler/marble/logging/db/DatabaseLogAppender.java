package com.ozerler.marble.logging.db;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.ozerler.marble.common.Constants;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Logback appender that sends logging events to the non-blocking {@link DatabaseLogQueue}.
 * Drops internal DB/ORM/Hikari loggers to prevent recursive logging loops.
 */
public class DatabaseLogAppender extends AppenderBase<ILoggingEvent> {

    private static final String[] IGNORED_LOGGERS = {
            "org.hibernate",
            "org.springframework.jdbc",
            "org.springframework.transaction",
            "org.springframework.orm",
            "com.zaxxer.hikari",
            "org.postgresql",
            "org.h2",
            "org.flywaydb",
            "com.ozerler.marble.logging.db"
    };

    @Override
    protected void append(ILoggingEvent event) {
        if (event == null) {
            return;
        }

        String loggerName = event.getLoggerName();
        if (shouldIgnoreLogger(loggerName)) {
            return;
        }

        try {
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.getTimeStamp()),
                    ZoneId.systemDefault()
            );

            String level = event.getLevel() != null ? event.getLevel().toString() : "INFO";
            String formattedMessage = event.getFormattedMessage();
            if (formattedMessage == null) {
                formattedMessage = "";
            }

            String exceptionClass = null;
            String exceptionMessage = null;
            String stackTrace = null;

            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                exceptionClass = truncate(throwableProxy.getClassName(), 255);
                exceptionMessage = throwableProxy.getMessage();
                stackTrace = ThrowableProxyUtil.asString(throwableProxy);
            }

            Map<String, String> mdc = event.getMDCPropertyMap();
            String username = null;
            String clientIp = null;
            String httpMethod = null;
            String requestUri = null;
            String correlationId = null;

            if (mdc != null && !mdc.isEmpty()) {
                username = truncate(mdc.get(Constants.MDC_USER_ID), 100);
                clientIp = truncate(mdc.get(Constants.MDC_CLIENT_IP), 100);
                httpMethod = truncate(mdc.get(Constants.MDC_HTTP_METHOD), 16);
                requestUri = truncate(mdc.get(Constants.MDC_REQUEST_URI), 500);
                correlationId = truncate(mdc.get(Constants.MDC_CORRELATION_ID), 64);
            }

            DatabaseLogEntry entry = new DatabaseLogEntry(
                    timestamp,
                    truncate(level, 16),
                    truncate(loggerName, 255),
                    formattedMessage,
                    exceptionClass,
                    exceptionMessage,
                    stackTrace,
                    username,
                    clientIp,
                    httpMethod,
                    requestUri,
                    correlationId,
                    truncate(event.getThreadName(), 100)
            );

            DatabaseLogQueue.getInstance().enqueue(entry);
        } catch (Exception ex) {
            // Guard against any appender failure affecting application flow
            addError("Failed to enqueue database log entry", ex);
        }
    }

    private boolean shouldIgnoreLogger(String loggerName) {
        if (loggerName == null) {
            return false;
        }
        for (String ignored : IGNORED_LOGGERS) {
            if (loggerName.startsWith(ignored)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String val, int maxLen) {
        if (val == null || val.length() <= maxLen) {
            return val;
        }
        return val.substring(0, maxLen);
    }
}
