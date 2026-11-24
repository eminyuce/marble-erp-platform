package com.ozerler.marble.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs scripts/ensure_secrets.sh against temporary directories: compose parsing, the
 * create/exists/abort decisions, and that no secret value ever reaches the output.
 */
class EnsureSecretsScriptTest {

    private static final Path SCRIPT = Path.of("scripts/ensure_secrets.sh");
    private static final Path PROD_COMPOSE = Path.of("docker-compose.prod.yml");
    private static final String HOST_DIR = "/opt/marble-erp/secrets";
    private static final String TOTP = "totp_encryption_key.txt";
    private static final String REMEMBER = "remember_me_key.txt";
    private static final List<String> REQUIRED =
            List.of("db_password.txt", "rustfs_access_key.txt", "rustfs_secret_key.txt");
    private static final int EXIT_ABORT = 3;
    private static final int TOTP_BYTES = 32;
    private static final int REMEMBER_BYTES = 48;

    @TempDir
    private Path dir;

    private record Run(int code, String output) {
    }

    @BeforeEach
    void requirePosixShell() {
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/bin/sh")), "needs a POSIX shell");
    }

    private Run run(Map<String, String> env, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("sh", SCRIPT.toString()));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().remove("SECRETS_DIR");
        builder.environment().putAll(env);
        Process process = builder.start();
        byte[] out = process.getInputStream().readAllBytes();
        Assertions.assertThat(process.waitFor(30, TimeUnit.SECONDS)).isTrue();
        return new Run(process.exitValue(), new String(out, StandardCharsets.UTF_8));
    }

    private Run preflight() throws IOException, InterruptedException {
        return run(Map.of(), "--dir", dir.toString(), "--host-dir", HOST_DIR, "--compose", PROD_COMPOSE.toString());
    }

    private void writeRequired() throws IOException {
        for (String name : REQUIRED) {
            Files.writeString(dir.resolve(name), "x");
        }
    }

    private static byte[] decoded(Path file) throws IOException {
        return Base64.getDecoder().decode(Files.readString(file).trim());
    }

    @Test
    @DisplayName("Compose parsing finds every host secret file, including x-marble-host-secrets")
    void listsComposeSecretFiles() throws Exception {
        Run run = run(Map.of(), "--dir", dir.toString(), "--host-dir", HOST_DIR,
                "--compose", PROD_COMPOSE.toString(), "--list");
        Assertions.assertThat(run.code()).isZero();
        Assertions.assertThat(run.output().lines().toList())
                .containsExactlyInAnyOrder("db_password.txt", REMEMBER, "rustfs_access_key.txt",
                        "rustfs_secret_key.txt", TOTP);
    }

    @Test
    @DisplayName("SECRETS_DIR from the environment replaces the compose default")
    void honoursSecretsDirOverride() throws Exception {
        Path compose = dir.resolve("compose.yml");
        Files.writeString(compose, String.join("\n",
                "services:",
                "  app:",
                "    secrets:",
                "      - db_password",
                "secrets:",
                "  db_password:",
                "    file: ${SECRETS_DIR:-/opt/marble-erp/secrets}/db_password.txt",
                "x-marble-host-secrets:",
                "  totp_encryption_key: \"${SECRETS_DIR:-/opt/marble-erp/secrets}/totp_encryption_key.txt\"",
                "networks:",
                "  n:",
                "    name: x",
                ""));
        Run custom = run(Map.of("SECRETS_DIR", "/srv/keys"), "--dir", dir.toString(), "--host-dir", "/srv/keys",
                "--compose", compose.toString(), "--list");
        Assertions.assertThat(custom.code()).isZero();
        Assertions.assertThat(custom.output().lines().toList()).containsExactly("db_password.txt", TOTP);

        Run mismatch = run(Map.of("SECRETS_DIR", "/srv/other"), "--dir", dir.toString(), "--host-dir", "/srv/keys",
                "--compose", compose.toString(), "--list");
        Assertions.assertThat(mismatch.code()).isEqualTo(EXIT_ABORT);
        Assertions.assertThat(mismatch.output()).contains("beklenen klasörde değil");
    }

    @Test
    @DisplayName("Missing TOTP and remember-me files are created with valid random values, never logged")
    void createsSafeFiles() throws Exception {
        writeRequired();
        Run run = preflight();

        Assertions.assertThat(run.code()).as(run.output()).isZero();
        Path totp = dir.resolve(TOTP);
        Path remember = dir.resolve(REMEMBER);
        Assertions.assertThat(Files.size(totp)).isPositive();
        Assertions.assertThat(decoded(totp)).hasSize(TOTP_BYTES);
        Assertions.assertThat(Files.readString(totp)).doesNotContain("\n");
        Assertions.assertThat(decoded(remember)).hasSize(REMEMBER_BYTES);
        Assertions.assertThat(Files.getPosixFilePermissions(totp))
                .isEqualTo(PosixFilePermissions.fromString("r--------"));
        Assertions.assertThat(run.output())
                .contains("[OLUŞTURULDU] " + TOTP, "[OLUŞTURULDU] " + REMEMBER, "[VAR] db_password.txt")
                .doesNotContain(Files.readString(totp).trim())
                .doesNotContain(Files.readString(remember).trim());
        try (var leftovers = Files.list(dir)) {
            Assertions.assertThat(leftovers.map(p -> p.getFileName().toString())).noneMatch(n -> n.startsWith("."));
        }
    }

    @Test
    @DisplayName("Running twice never changes an existing value")
    void idempotent() throws Exception {
        writeRequired();
        Assertions.assertThat(preflight().code()).isZero();
        byte[] totpBefore = Files.readAllBytes(dir.resolve(TOTP));
        byte[] rememberBefore = Files.readAllBytes(dir.resolve(REMEMBER));

        Run second = preflight();

        Assertions.assertThat(second.code()).isZero();
        Assertions.assertThat(Files.readAllBytes(dir.resolve(TOTP))).isEqualTo(totpBefore);
        Assertions.assertThat(Files.readAllBytes(dir.resolve(REMEMBER))).isEqualTo(rememberBefore);
        Assertions.assertThat(second.output()).contains("[VAR] " + TOTP).doesNotContain("OLUŞTURULDU")
                .doesNotContain(new String(totpBefore, StandardCharsets.UTF_8).trim());
    }

    @Test
    @DisplayName("An empty or whitespace-only file is never overwritten; the deploy aborts")
    void emptyFileAborts() throws Exception {
        writeRequired();
        Files.writeString(dir.resolve(TOTP), "");
        Files.writeString(dir.resolve(REMEMBER), "  \n\t\n");

        Run run = preflight();

        Assertions.assertThat(run.code()).isEqualTo(EXIT_ABORT);
        Assertions.assertThat(Files.size(dir.resolve(TOTP))).isZero();
        Assertions.assertThat(Files.readString(dir.resolve(REMEMBER))).isEqualTo("  \n\t\n");
        Assertions.assertThat(run.output()).contains(TOTP + " var ama boş", REMEMBER + " var ama boş",
                "Yayın durduruldu");
    }

    @Test
    @DisplayName("A missing file that must match existing data aborts and nothing is generated")
    void missingRequiredAborts() throws Exception {
        Files.writeString(dir.resolve("rustfs_access_key.txt"), "x");
        Files.writeString(dir.resolve("rustfs_secret_key.txt"), "x");

        Run run = preflight();

        Assertions.assertThat(run.code()).isEqualTo(EXIT_ABORT);
        Assertions.assertThat(run.output())
                .contains("Zorunlu gizli dosya eksik: " + HOST_DIR + "/db_password.txt")
                .contains("otomatik üretilmez");
        Assertions.assertThat(dir.resolve("db_password.txt")).doesNotExist();
        Assertions.assertThat(dir.resolve(TOTP)).doesNotExist();
        Assertions.assertThat(dir.resolve(REMEMBER)).doesNotExist();
    }

    @Test
    @DisplayName("An existing TOTP file that is not a 32-byte key aborts and is left alone")
    void invalidTotpAborts() throws Exception {
        writeRequired();
        Files.writeString(dir.resolve(REMEMBER), "x");
        Files.writeString(dir.resolve(TOTP), "not-a-key");

        Run run = preflight();

        Assertions.assertThat(run.code()).isEqualTo(EXIT_ABORT);
        Assertions.assertThat(Files.readString(dir.resolve(TOTP))).isEqualTo("not-a-key");
        Assertions.assertThat(run.output()).contains("geçerli bir AES-256 anahtarı değil").doesNotContain("not-a-key");
    }

    @Test
    @DisplayName("Existing files are reported by name only, never by value")
    void existingValuesNotLogged() throws Exception {
        writeRequired();
        String key = Base64.getEncoder().encodeToString(new byte[TOTP_BYTES]);
        Files.writeString(dir.resolve(TOTP), key + "\n");
        Files.writeString(dir.resolve(REMEMBER), "remember-value-for-test");

        Run run = preflight();

        Assertions.assertThat(run.code()).isZero();
        Assertions.assertThat(Files.readString(dir.resolve(TOTP))).isEqualTo(key + "\n");
        Assertions.assertThat(run.output()).doesNotContain(key).doesNotContain("remember-value-for-test");
    }
}
