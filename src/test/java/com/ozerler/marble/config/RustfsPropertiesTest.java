package com.ozerler.marble.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RustfsPropertiesTest {

    @Test
    @DisplayName("blank access key or secret key fails fast and does not echo the secret")
    void requireCredentials_BlankKeyFailsWithoutEchoingSecret() {
        RustfsProperties properties = new RustfsProperties();
        properties.setAccessKey(" ");
        properties.setSecretKey("");

        assertThatThrownBy(properties::requireCredentials)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RUSTFS_ACCESS_KEY")
                .hasMessageNotContaining("marblerustfs");
    }
}
