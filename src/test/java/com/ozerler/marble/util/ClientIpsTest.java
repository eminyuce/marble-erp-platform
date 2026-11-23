package com.ozerler.marble.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpsTest {

    @Test
    @DisplayName("prefers the first X-Forwarded-For address")
    void prefersForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        Assertions.assertThat(ClientIps.from(request)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("falls back to X-Real-IP when forwarded header is blank")
    void fallsBackToRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "  ");
        request.addHeader("X-Real-IP", "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        Assertions.assertThat(ClientIps.from(request)).isEqualTo("198.51.100.20");
    }

    @Test
    @DisplayName("uses remote address when proxy headers are absent")
    void usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        Assertions.assertThat(ClientIps.from(request)).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("returns empty string for a null request")
    void nullRequest() {
        Assertions.assertThat(ClientIps.from(null)).isEmpty();
    }
}
