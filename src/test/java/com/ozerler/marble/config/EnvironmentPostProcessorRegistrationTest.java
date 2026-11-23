package com.ozerler.marble.config;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;

class EnvironmentPostProcessorRegistrationTest {

    @Test
    @DisplayName("SpringFactoriesLoader loads the secret-file and database-URL post-processors")
    void springFactoriesLoaderDiscoversApplicationPostProcessors() {
        List<String> names = SpringFactoriesLoader.loadFactoryNames(
                EnvironmentPostProcessor.class,
                EnvironmentPostProcessorRegistrationTest.class.getClassLoader());

        Assertions.assertThat(names)
                .contains(
                        DatabaseUrlEnvironmentPostProcessor.class.getName(),
                        SecretFileEnvironmentPostProcessor.class.getName());
    }
}
