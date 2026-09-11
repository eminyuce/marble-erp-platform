package com.ozerler.marble.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpsTest {

    @Test
    @DisplayName("prefers the first X-Forwarded-For address")
    void prefersForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIps.from(request)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("falls back to X-Real-IP when forwarded header is blank")
    void fallsBackToRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "  ");
        request.addHeader("X-Real-IP", "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIps.from(request)).isEqualTo("198.51.100.20");
    }

    @Test
    @DisplayName("uses remote address when proxy headers are absent")
    void usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIps.from(request)).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("returns empty string for a null request")
    void nullRequest() {
        assertThat(ClientIps.from(null)).isEmpty();
    }
}
