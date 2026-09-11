package com.ozerler.marble.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestsTest {

    @Test
    @DisplayName("isHtmx is true only when HX-Request is true")
    void isHtmxReadsHeader() {
        MockHttpServletRequest htmx = new MockHttpServletRequest();
        htmx.addHeader("HX-Request", "true");
        assertThat(HttpRequests.isHtmx(htmx)).isTrue();

        MockHttpServletRequest fullPage = new MockHttpServletRequest();
        assertThat(HttpRequests.isHtmx(fullPage)).isFalse();
        assertThat(HttpRequests.isHtmx(null)).isFalse();
    }

    @Test
    @DisplayName("expectsJson reads Accept and X-Requested-With")
    void expectsJsonFromHeaders() {
        MockHttpServletRequest json = new MockHttpServletRequest();
        json.addHeader("Accept", "application/json");
        assertThat(HttpRequests.expectsJson(json)).isTrue();

        MockHttpServletRequest xhr = new MockHttpServletRequest();
        xhr.addHeader("X-Requested-With", "XMLHttpRequest");
        assertThat(HttpRequests.expectsJson(xhr)).isTrue();

        MockHttpServletRequest html = new MockHttpServletRequest();
        html.addHeader("Accept", "text/html");
        assertThat(HttpRequests.expectsJson(html)).isFalse();
    }
}
