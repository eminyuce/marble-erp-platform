package com.ozerler.marble.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RustfsPropertiesTest {

    @Test
    @DisplayName("blank access key or secret key fails fast and does not echo the secret")
    void requireCredentialsBlankKeyFailsWithoutEchoingSecret() {
        RustfsProperties properties = new RustfsProperties();
        properties.setAccessKey(" ");
        properties.setSecretKey("");

        Assertions.assertThatThrownBy(properties::requireCredentials)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RUSTFS_ACCESS_KEY")
                .hasMessageNotContaining("marblerustfs");
    }
}
