package com.ozerler.marble.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> errors
) {

    public ErrorResponse {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(path, "path");
        errors = (errors == null || errors.isEmpty()) ? null : Map.copyOf(errors);
    }

    public static ErrorResponse of(HttpStatus httpStatus, String message, String path) {
        return of(httpStatus, message, path, null);
    }

    public static ErrorResponse of(HttpStatus httpStatus, String message, String path, Map<String, String> errors) {
        return new ErrorResponse(
                Instant.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                path,
                errors
        );
    }
}
