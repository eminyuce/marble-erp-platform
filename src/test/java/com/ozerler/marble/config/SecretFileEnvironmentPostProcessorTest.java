package com.ozerler.marble.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretFileEnvironmentPostProcessorTest {

    @TempDir
    Path directory;

    @Test
    @DisplayName("a readable secret file loads and drops one trailing newline")
    void readsSecretAndTrimsOneNewline() throws Exception {
        Path file = directory.resolve("db_password.txt");
        Files.writeString(file, "correct-horse\n", StandardCharsets.UTF_8);

        assertThat(SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isEqualTo("correct-horse");
    }

    @Test
    @DisplayName("an empty secret file is rejected")
    void emptyFileIsRejected() throws Exception {
        Path file = directory.resolve("empty.txt");
        Files.writeString(file, "\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("a missing secret file is rejected")
    void missingFileIsRejected() {
        Path file = directory.resolve("missing.txt");

        assertThatThrownBy(() -> SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("readable");
    }

    @Test
    @DisplayName("an unreadable secret file is rejected")
    void unreadableFileIsRejected() throws Exception {
        Path file = directory.resolve("locked.txt");
        Files.writeString(file, "secret\n", StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(file, EnumSet.noneOf(PosixFilePermission.class));

        assertThatThrownBy(() -> SecretFileEnvironmentPostProcessor.readSecret("DB_PASSWORD_FILE", file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("readable");
    }
}
