package com.ozerler.marble.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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

        Assertions.assertThat(sanitized.getParameter("page")).isNull();
        Assertions.assertThat(sanitized.getParameterValues("page")).isNull();
        Assertions.assertThat(sanitized.getParameter("size")).isNull();
        Assertions.assertThat(sanitized.getParameter("search")).isEqualTo("SIP");
        Assertions.assertThat(sanitized.getParameterMap()).containsOnlyKeys("search");
    }

    @Test
    @DisplayName("numeric page and size values are left unchanged")
    void keepsNumericPaging() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("page", "2");
        request.setParameter("size", "25");

        TabulatorQuerySanitizerFilter.SanitizedRequest sanitized =
                new TabulatorQuerySanitizerFilter.SanitizedRequest(request);

        Assertions.assertThat(sanitized.getParameter("page")).isEqualTo("2");
        Assertions.assertThat(sanitized.getParameter("size")).isEqualTo("25");
        Assertions.assertThat(sanitized.getParameterMap()).containsEntry("page", new String[]{"2"});
    }
}
