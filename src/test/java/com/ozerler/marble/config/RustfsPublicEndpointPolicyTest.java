package com.ozerler.marble.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RustfsPublicEndpointPolicyTest {

    @Test
    @DisplayName("production accepts an https public host")
    void httpsPublicHostIsAccepted() {
        assertThatCode(() -> RustfsPublicEndpointPolicy.requireHttpsPublicHost("https://files.example.com"))
                .doesNotThrowAnyException();
        assertThatCode(() -> RustfsPublicEndpointPolicy.requireHttpsPublicHost("https://ozerler.naklink.com"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("production rejects http, internal hosts, and loopback")
    void unsafePublicEndpointsAreRejected() {
        assertThatThrownBy(() -> RustfsPublicEndpointPolicy.requireHttpsPublicHost("http://files.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("https");
        assertThatThrownBy(() -> RustfsPublicEndpointPolicy.requireHttpsPublicHost("https://rustfs:9000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("public hostname");
        assertThatThrownBy(() -> RustfsPublicEndpointPolicy.requireHttpsPublicHost("https://localhost:9000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("public hostname");
        assertThatThrownBy(() -> RustfsPublicEndpointPolicy.requireHttpsPublicHost("http://127.0.0.1:9000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("https");
    }
}
