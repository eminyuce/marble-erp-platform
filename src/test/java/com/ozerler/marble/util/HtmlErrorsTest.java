package com.ozerler.marble.util;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HtmlErrorsTest {

    @Test
    void mapsCommonStatusesToTurkishTitlesAndIcons() {
        Assertions.assertThat(HtmlErrors.titleFor(404)).isEqualTo("Sayfa bulunamadı");
        Assertions.assertThat(HtmlErrors.titleFor(403)).isEqualTo("Erişim engellendi");
        Assertions.assertThat(HtmlErrors.titleFor(500)).isEqualTo("Sunucu hatası");
        Assertions.assertThat(HtmlErrors.iconFor(404)).isEqualTo("search-x");
        Assertions.assertThat(HtmlErrors.iconFor(500)).isEqualTo("server-crash");
        Assertions.assertThat(HtmlErrors.toneFor(404)).isEqualTo("missing");
        Assertions.assertThat(HtmlErrors.toneFor(500)).isEqualTo("server");
        Assertions.assertThat(HtmlErrors.toneFor(403)).isEqualTo("forbidden");
    }

    @Test
    void prefersServerMessageWhenPresent() {
        Assertions.assertThat(HtmlErrors.messageFor(404, "No static resource admin/missing."))
                .isEqualTo("No static resource admin/missing.");
        Assertions.assertThat(HtmlErrors.messageFor(404, "No message available"))
                .isEqualTo("İstenen sayfa veya kaynak bulunamadı.");
        Assertions.assertThat(HtmlErrors.messageFor(500, null))
                .isEqualTo("Beklenmeyen bir sunucu hatası oluştu. Ayrıntılar aşağıda.");
    }

    @Test
    void prefersOriginalErrorRequestUriOverErrorPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/admin/missing-page");
        request.setQueryString("ref=nav");

        Assertions.assertThat(HtmlErrors.pathOf(request, "/error")).isEqualTo("/admin/missing-page");
        Assertions.assertThat(HtmlErrors.methodOf(request)).isEqualTo("GET");
        Assertions.assertThat(HtmlErrors.queryStringOf(request)).isEqualTo("ref=nav");
    }

    @Test
    void buildsDeveloperPageFromAttributesAndException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/error");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/admin/boom");
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("message", "Developer error-page probe");
        attributes.put("path", "/error");
        attributes.put("timestamp", LocalDateTime.of(2026, 9, 11, 18, 5));
        IllegalStateException error = new IllegalStateException("Developer error-page probe");

        ErrorPageDetails page = HtmlErrors.page(500, "Internal Server Error", attributes, request, error);

        Assertions.assertThat(page.status()).isEqualTo(500);
        Assertions.assertThat(page.title()).isEqualTo("Sunucu hatası");
        Assertions.assertThat(page.path()).isEqualTo("/admin/boom");
        Assertions.assertThat(page.method()).isEqualTo("POST");
        Assertions.assertThat(page.exceptionType()).isEqualTo("java.lang.IllegalStateException");
        Assertions.assertThat(page.hasStackTrace()).isTrue();
        Assertions.assertThat(page.stackTrace()).contains("IllegalStateException");
        Assertions.assertThat(page.timestamp()).isEqualTo("2026-09-11 18:05");
        Assertions.assertThat(page.retryHref()).isEqualTo("/admin/boom");
    }

    @Test
    void retryHrefFallsBackToDashboardForBareErrorPath() {
        ErrorPageDetails page = new ErrorPageDetails(
                500, "Internal Server Error", "Sunucu hatası", "server-crash", "server",
                "msg", "/error", "GET", "", "2026-09-11 18:00", "", "");
        Assertions.assertThat(page.retryHref()).isEqualTo("/admin/dashboard");
    }
}
