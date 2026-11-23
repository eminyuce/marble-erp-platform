package com.ozerler.marble.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HttpRequestsTest {

    @Test
    @DisplayName("isHtmx is true only when HX-Request is true")
    void isHtmxReadsHeader() {
        MockHttpServletRequest htmx = new MockHttpServletRequest();
        htmx.addHeader("HX-Request", "true");
        Assertions.assertThat(HttpRequests.isHtmx(htmx)).isTrue();

        MockHttpServletRequest fullPage = new MockHttpServletRequest();
        Assertions.assertThat(HttpRequests.isHtmx(fullPage)).isFalse();
        Assertions.assertThat(HttpRequests.isHtmx(null)).isFalse();
    }

    @Test
    @DisplayName("expectsJson reads Accept and X-Requested-With")
    void expectsJsonFromHeaders() {
        MockHttpServletRequest json = new MockHttpServletRequest();
        json.addHeader("Accept", "application/json");
        Assertions.assertThat(HttpRequests.expectsJson(json)).isTrue();

        MockHttpServletRequest xhr = new MockHttpServletRequest();
        xhr.addHeader("X-Requested-With", "XMLHttpRequest");
        Assertions.assertThat(HttpRequests.expectsJson(xhr)).isTrue();

        MockHttpServletRequest html = new MockHttpServletRequest();
        html.addHeader("Accept", "text/html");
        Assertions.assertThat(HttpRequests.expectsJson(html)).isFalse();
    }
}
