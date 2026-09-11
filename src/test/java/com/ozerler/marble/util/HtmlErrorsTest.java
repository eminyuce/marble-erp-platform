package com.ozerler.marble.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.RequestDispatcher;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlErrorsTest {

    @Test
    void mapsCommonStatusesToTurkishTitlesAndIcons() {
        assertThat(HtmlErrors.titleFor(404)).isEqualTo("Sayfa bulunamadı");
        assertThat(HtmlErrors.titleFor(403)).isEqualTo("Erişim engellendi");
        assertThat(HtmlErrors.titleFor(500)).isEqualTo("Sunucu hatası");
        assertThat(HtmlErrors.iconFor(404)).isEqualTo("search-x");
        assertThat(HtmlErrors.iconFor(500)).isEqualTo("server-crash");
        assertThat(HtmlErrors.toneFor(404)).isEqualTo("missing");
        assertThat(HtmlErrors.toneFor(500)).isEqualTo("server");
        assertThat(HtmlErrors.toneFor(403)).isEqualTo("forbidden");
    }

    @Test
    void prefersServerMessageWhenPresent() {
        assertThat(HtmlErrors.messageFor(404, "No static resource admin/missing."))
                .isEqualTo("No static resource admin/missing.");
        assertThat(HtmlErrors.messageFor(404, "No message available"))
                .isEqualTo("İstenen sayfa veya kaynak bulunamadı.");
        assertThat(HtmlErrors.messageFor(500, null))
                .isEqualTo("Beklenmeyen bir sunucu hatası oluştu. Ayrıntılar aşağıda.");
    }

    @Test
    void prefersOriginalErrorRequestUriOverErrorPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/admin/missing-page");
        request.setQueryString("ref=nav");

        assertThat(HtmlErrors.pathOf(request, "/error")).isEqualTo("/admin/missing-page");
        assertThat(HtmlErrors.methodOf(request)).isEqualTo("GET");
        assertThat(HtmlErrors.queryStringOf(request)).isEqualTo("ref=nav");
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

        assertThat(page.status()).isEqualTo(500);
        assertThat(page.title()).isEqualTo("Sunucu hatası");
        assertThat(page.path()).isEqualTo("/admin/boom");
        assertThat(page.method()).isEqualTo("POST");
        assertThat(page.exceptionType()).isEqualTo("java.lang.IllegalStateException");
        assertThat(page.hasStackTrace()).isTrue();
        assertThat(page.stackTrace()).contains("IllegalStateException");
        assertThat(page.timestamp()).isEqualTo("2026-09-11 18:05");
        assertThat(page.retryHref()).isEqualTo("/admin/boom");
    }

    @Test
    void retryHrefFallsBackToDashboardForBareErrorPath() {
        ErrorPageDetails page = new ErrorPageDetails(
                500, "Internal Server Error", "Sunucu hatası", "server-crash", "server",
                "msg", "/error", "GET", "", "2026-09-11 18:00", "", "");
        assertThat(page.retryHref()).isEqualTo("/admin/dashboard");
    }
}
