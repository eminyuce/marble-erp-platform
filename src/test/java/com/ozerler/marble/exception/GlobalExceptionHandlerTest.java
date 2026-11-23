package com.ozerler.marble.exception;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/upload");
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with the exception message and no field errors")
    void handleResourceNotFoundReturnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Blok", 42L);

        ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(ex, request);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.getStatus()).isEqualTo(404);
        Assertions.assertThat(body.getTitle()).isEqualTo("Not Found");
        Assertions.assertThat(body.getDetail()).isEqualTo("Blok bulunamadı: 42");
        Assertions.assertThat(body.getInstance()).hasToString("/api/upload");
        Assertions.assertThat(body.getProperties()).isNull();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns 400 with a field-to-message map")
    void handleValidationReturnsBadRequestWithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Geçerli bir e-posta adresi giriniz"));
        bindingResult.addError(new FieldError("request", "username", "Missing required field: username"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleValidation(ex, request);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.getStatus()).isEqualTo(400);
        Assertions.assertThat(body.getTitle()).isEqualTo("Bad Request");
        Assertions.assertThat(body.getDetail()).isEqualTo("Validation failed");
        Assertions.assertThat(body.getInstance()).hasToString("/api/upload");
        Assertions.assertThat(body.getProperties().get("errors"))
                .isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("AccessDeniedException returns 403 without leaking authorization details")
    void handleAccessDeniedReturnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("User is not ADMIN");

        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(ex, request);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ProblemDetail body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.getStatus()).isEqualTo(403);
        Assertions.assertThat(body.getTitle()).isEqualTo("Forbidden");
        Assertions.assertThat(body.getDetail()).isEqualTo("Access denied");
        Assertions.assertThat(body.getInstance()).hasToString("/api/upload");
    }

    @Test
    @DisplayName("Unhandled Exception returns 500 with a generic message and no stack trace in the body")
    void handleUnexpectedHidesInternalDetails() {
        Exception ex = new IllegalStateException("secret database url leaked");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(ex, request);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.getStatus()).isEqualTo(500);
        Assertions.assertThat(body.getTitle()).isEqualTo("Internal Server Error");
        Assertions.assertThat(body.getDetail()).isEqualTo("An unexpected error occurred");
        Assertions.assertThat(body.getDetail()).doesNotContain("secret", "database");
        Assertions.assertThat(body.getInstance()).hasToString("/api/upload");
    }

    @Test
    @DisplayName("ResourceNotFoundException message constructor preserves the given text")
    void resourceNotFoundMessageConstructor() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Plaka bulunamadı: S-100");
        Assertions.assertThat(ex.getMessage()).isEqualTo("Plaka bulunamadı: S-100");
    }
}
