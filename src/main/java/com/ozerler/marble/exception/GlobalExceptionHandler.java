package com.ozerler.marble.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final String GENERIC_INTERNAL_ERROR = "An unexpected error occurred";
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String ACCESS_DENIED = "Access denied";
    private static final String INVALID_VALUE = "Invalid value";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.info("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = fieldErrors(ex);
        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, request, fieldErrors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.FORBIDDEN, ACCESS_DENIED, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_INTERNAL_ERROR, request, null);
    }

    private static Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : INVALID_VALUE;
            errors.putIfAbsent(fieldError.getField(), message);
        }
        return errors;
    }

    private static ResponseEntity<ErrorResponse> respond(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, message, request.getRequestURI(), errors));
    }
}
