package com.ozerler.marble.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

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
    void handleResourceNotFound_ReturnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Blok", 42L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("Blok bulunamadı: 42");
        assertThat(body.path()).isEqualTo("/api/upload");
        assertThat(body.errors()).isNull();
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns 400 with a field-to-message map")
    void handleValidation_ReturnsBadRequestWithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Geçerli bir e-posta adresi giriniz"));
        bindingResult.addError(new FieldError("request", "username", "Missing required field: username"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Validation failed");
        assertThat(body.path()).isEqualTo("/api/upload");
        assertThat(body.errors())
                .containsEntry("email", "Geçerli bir e-posta adresi giriniz")
                .containsEntry("username", "Missing required field: username")
                .hasSize(2);
    }

    @Test
    @DisplayName("AccessDeniedException returns 403 without leaking authorization details")
    void handleAccessDenied_ReturnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("User is not ADMIN");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(403);
        assertThat(body.error()).isEqualTo("Forbidden");
        assertThat(body.message()).isEqualTo("Access denied");
        assertThat(body.path()).isEqualTo("/api/upload");
        assertThat(body.errors()).isNull();
    }

    @Test
    @DisplayName("Unhandled Exception returns 500 with a generic message and no stack trace in the body")
    void handleUnexpected_HidesInternalDetails() {
        Exception ex = new IllegalStateException("secret database url leaked");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        assertThat(body.message()).doesNotContain("secret", "database");
        assertThat(body.path()).isEqualTo("/api/upload");
        assertThat(body.errors()).isNull();
    }

    @Test
    @DisplayName("ResourceNotFoundException message constructor preserves the given text")
    void resourceNotFound_MessageConstructor() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Plaka bulunamadı: S-100");
        assertThat(ex.getMessage()).isEqualTo("Plaka bulunamadı: S-100");
    }
}
