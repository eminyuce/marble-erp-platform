package com.ozerler.marble.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvironmentPostProcessorRegistrationTest {

    @Test
    @DisplayName("META-INF/spring.factories is on the classpath and lists both post-processors")
    void springFactoriesResourceIsOnTheClasspath() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories")) {
            Assertions.assertThat(in).as("META-INF/spring.factories").isNotNull();
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Assertions.assertThat(body)
                    .contains(SecretFileEnvironmentPostProcessor.class.getName())
                    .contains(DatabaseUrlEnvironmentPostProcessor.class.getName());
        }
    }
}
