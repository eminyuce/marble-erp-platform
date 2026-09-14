package com.ozerler.marble.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TabulatorQuerySanitizerFilterTest {

    @Test
    @DisplayName("page=undefined and size=null are treated as missing parameters")
    void dropsTabulatorPlaceholderTokens() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("page", "undefined");
        request.setParameter("size", "null");
        request.setParameter("search", "SIP");

        TabulatorQuerySanitizerFilter.SanitizedRequest sanitized =
                new TabulatorQuerySanitizerFilter.SanitizedRequest(request);

        assertThat(sanitized.getParameter("page")).isNull();
        assertThat(sanitized.getParameterValues("page")).isNull();
        assertThat(sanitized.getParameter("size")).isNull();
        assertThat(sanitized.getParameter("search")).isEqualTo("SIP");
        assertThat(sanitized.getParameterMap()).containsOnlyKeys("search");
    }

    @Test
    @DisplayName("numeric page and size values are left unchanged")
    void keepsNumericPaging() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("page", "2");
        request.setParameter("size", "25");

        TabulatorQuerySanitizerFilter.SanitizedRequest sanitized =
                new TabulatorQuerySanitizerFilter.SanitizedRequest(request);

        assertThat(sanitized.getParameter("page")).isEqualTo("2");
        assertThat(sanitized.getParameter("size")).isEqualTo("25");
        assertThat(sanitized.getParameterMap()).containsEntry("page", new String[]{"2"});
    }
}
