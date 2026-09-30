package com.ozerler.marble.logging.db;

import java.time.LocalDateTime;

/**
 * Immutable record carrying log event data to be persisted in database.
 */
public record DatabaseLogEntry(
        LocalDateTime timestamp,
        String level,
        String loggerName,
        String message,
        String exceptionClass,
        String exceptionMessage,
        String stackTrace,
        String username,
        String clientIp,
        String httpMethod,
        String requestUri,
        String correlationId,
        String threadName
) {
}
