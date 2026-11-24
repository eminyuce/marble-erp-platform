package com.ozerler.marble.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * reCAPTCHA is gone: no code, template, config or script may mention it or load a Google script.
 * Old Flyway migrations are history and V39 deletes the settings, so migrations are exempt.
 */
class RecaptchaRemovedTest {

    private static final List<Path> ROOTS = List.of(
            Path.of("src/main/java"), Path.of("src/main/resources"), Path.of("scripts"));

    private static boolean scanned(Path path) {
        String name = path.toString().replace('\\', '/');
        return Files.isRegularFile(path)
                && !name.contains("/db/migration/")
                && !name.contains("/static/vendor/")
                && (name.endsWith(".java") || name.endsWith(".html") || name.endsWith(".js")
                        || name.endsWith(".yml") || name.endsWith(".properties") || name.endsWith(".sh")
                        || name.endsWith(".sql"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        } catch (IOException ex) {
            return "";
        }
    }

    @Test
    @DisplayName("No source file mentions reCAPTCHA or loads google.com/recaptcha")
    void noRecaptchaLeft() throws IOException {
        for (Path root : ROOTS) {
            try (Stream<Path> files = Files.walk(root)) {
                List<Path> offenders = files.filter(RecaptchaRemovedTest::scanned)
                        .filter(p -> {
                            String text = read(p);
                            return text.contains("recaptcha") || text.contains("www.google.com/recaptcha")
                                    || text.contains("gstatic.com/recaptcha");
                        })
                        .toList();
                Assertions.assertThat(offenders).as("files still mentioning reCAPTCHA under " + root).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("V39 removes the stored reCAPTCHA settings")
    void migrationDeletesSettings() throws IOException {
        String sql = Files.readString(
                Path.of("src/main/resources/db/migration/V39__totp_two_factor_and_remove_recaptcha.sql"));
        Assertions.assertThat(sql.toLowerCase(Locale.ROOT))
                .contains("delete from system_settings")
                .contains("recaptcha");
    }
}
