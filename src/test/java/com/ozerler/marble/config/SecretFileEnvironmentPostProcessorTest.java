package com.ozerler.marble.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class SecretFileEnvironmentPostProcessorTest {

    @TempDir
    Path directory;

    @Test
    @DisplayName("a readable secret file loads and drops one trailing newline")
    void readsSecretAndTrimsOneNewline() throws Exception {
        Path file = directory.resolve("db_password.txt");
        Files.writeString(file, "correct-horse\n", StandardCharsets.UTF_8);

        Assertions.assertThat(SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isEqualTo("correct-horse");
    }

    @Test
    @DisplayName("an empty secret file is rejected")
    void emptyFileIsRejected() throws Exception {
        Path file = directory.resolve("empty.txt");
        Files.writeString(file, "\n", StandardCharsets.UTF_8);

        Assertions.assertThatThrownBy(() -> SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("a missing secret file is rejected")
    void missingFileIsRejected() {
        Path file = directory.resolve("missing.txt");

        Assertions.assertThatThrownBy(() -> SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("readable");
    }

    @Test
    @DisplayName("the prod profile resolves spring.datasource.password from DB_PASSWORD_FILE")
    void prodProfileResolvesDatasourcePasswordFromFile() throws IOException {
        Path file = directory.resolve("db_password.txt");
        Files.writeString(file, "correct-horse\n", StandardCharsets.UTF_8);
        ConfigurableEnvironment environment = new StandardEnvironment() {
            @Override
            public Map<String, Object> getSystemEnvironment() {
                return Map.of("DB_PASSWORD_FILE", file.toString());
            }
        };
        new SecretFileEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yml"))
                .forEach(source -> environment.getPropertySources().addLast(source));

        Assertions.assertThat(environment.getProperty("DB_PASSWORD")).isEqualTo("correct-horse");
        Assertions.assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("correct-horse");
    }

    @Test
    @DisplayName("an unreadable secret file is rejected")
    void unreadableFileIsRejected() throws Exception {
        Path file = directory.resolve("locked.txt");
        Files.writeString(file, "secret\n", StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(file, EnumSet.noneOf(PosixFilePermission.class));

        Assertions.assertThatThrownBy(() -> SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("readable");
    }
}
