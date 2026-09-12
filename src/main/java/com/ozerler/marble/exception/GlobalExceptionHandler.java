package com.ozerler.marble.exception;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.util.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler() {
        this(null);
    }

    @Autowired
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request, Locale locale) {
        log.info("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return handleResourceNotFound(ex, request, LocaleContextHolder.getLocale());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request, Locale locale) {
        Map<String, String> fieldErrors = fieldErrors(ex, locale);
        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);
        String validationFailedMsg = resolveMessage("error.validation.failed", null, Constants.ERROR_VALIDATION_FAILED, locale);
        return respond(HttpStatus.BAD_REQUEST, validationFailedMsg, request, fieldErrors);
    }

    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        return handleValidation(ex, request, LocaleContextHolder.getLocale());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request, Locale locale) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        String accessDeniedMsg = resolveMessage("error.access.denied", null, Constants.ERROR_ACCESS_DENIED, locale);
        return respond(HttpStatus.FORBIDDEN, accessDeniedMsg, request, null);
    }

    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return handleAccessDenied(ex, request, LocaleContextHolder.getLocale());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request, Locale locale) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        String genericInternalMsg = resolveMessage("error.generic.internal", null, Constants.ERROR_GENERIC_INTERNAL, locale);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, genericInternalMsg, request, null);
    }

    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        return handleUnexpected(ex, request, LocaleContextHolder.getLocale());
    }

    private Map<String, String> fieldErrors(MethodArgumentNotValidException ex, Locale locale) {
        Map<String, String> errors = new LinkedHashMap<>();
        Locale activeLocale = locale != null ? locale : LocaleContextHolder.getLocale();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = null;
            if (messageSource != null) {
                try {
                    message = messageSource.getMessage(fieldError, activeLocale);
                } catch (Exception ignored) {
                }
            }
            if (message == null || message.isBlank()) {
                message = fieldError.getDefaultMessage();
            }
            if (message == null || message.isBlank()) {
                message = resolveMessage("error.invalid.value", null, Constants.ERROR_INVALID_VALUE, activeLocale);
            }
            errors.putIfAbsent(fieldError.getField(), message);
        }
        return errors;
    }

    private String resolveMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        Locale activeLocale = locale != null ? locale : LocaleContextHolder.getLocale();
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, defaultMessage, activeLocale);
            } catch (Exception ignored) {
            }
        }
        return MessageUtils.getMessage(code, activeLocale, args);
    }

    private static ResponseEntity<ErrorResponse> respond(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, message, request.getRequestURI(), errors));
    }
}
