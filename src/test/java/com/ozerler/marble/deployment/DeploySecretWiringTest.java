package com.ozerler.marble.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The deploy must check secrets before "compose up", and the TOTP key must not be a compose
 * secret (a missing compose secret file stops "up" before the container exists).
 */
class DeploySecretWiringTest {

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Test
    @DisplayName("Compose no longer requires the TOTP key as a file secret but still lists it for the preflight")
    void composeWiring() throws IOException {
        String compose = read("docker-compose.prod.yml");
        String appSecrets = compose.substring(compose.indexOf("  app:"), compose.indexOf("\nsecrets:"));
        Assertions.assertThat(appSecrets)
                .doesNotContain("- totp_encryption_key")
                .contains("TOTP_ENCRYPTION_KEY_FILE: /tmp/marble-secrets/totp_encryption_key")
                .contains("MARBLE_HOST_SECRETS_DIR:");
        String topLevelSecrets = compose.substring(compose.indexOf("\nsecrets:"),
                compose.indexOf("\nx-marble-host-secrets:"));
        Assertions.assertThat(topLevelSecrets).doesNotContain("totp_encryption_key");
        Assertions.assertThat(compose).contains("x-marble-host-secrets:\n  totp_encryption_key: ");
    }

    @Test
    @DisplayName("The image ships the preflight script and the entrypoint prepares the TOTP key before Java")
    void imageWiring() throws IOException {
        Assertions.assertThat(read("Dockerfile")).contains("scripts/ensure_secrets.sh /stack/scripts/");
        String entrypoint = read("scripts/docker-entrypoint.sh");
        Assertions.assertThat(entrypoint.indexOf("ensure_totp_key\nfi"))
                .isPositive()
                .isLessThan(entrypoint.lastIndexOf("exec java"));
        Assertions.assertThat(entrypoint).contains("< /stack/scripts/ensure_secrets.sh");
    }

    @Test
    @DisplayName("The in-app rollout runs the preflight before pull and up, and keeps the rollback")
    void rolloutRunsPreflightFirst() throws IOException {
        String script = read("scripts/deploy_from_app.sh");
        int preflight = script.indexOf("if ! preflight_secrets; then");
        int composePull = script.indexOf("\\$STACK/docker-compose.prod.yml\" pull app");
        int composeUp = script.indexOf("\\$STACK/docker-compose.prod.yml\" up -d --no-deps app");
        Assertions.assertThat(preflight).isPositive().isLessThan(composePull).isLessThan(composeUp);
        Assertions.assertThat(script).contains("/stack/scripts/ensure_secrets.sh").contains("rollback-previous");

        int hostPreflight = script.indexOf("sh \"$REPO_ROOT/scripts/ensure_secrets.sh\"");
        int hostPull = script.indexOf("docker compose \"${compose_args[@]}\" pull app");
        Assertions.assertThat(hostPreflight).isPositive().isLessThan(hostPull);
    }

    @Test
    @DisplayName("Host scripts never overwrite an existing (even empty) secret file")
    void hostScriptsKeepExistingFiles() throws IOException {
        Assertions.assertThat(read("scripts/deploy_production.sh")).contains("var ama boş. Üzerine yazılmadı");
        Assertions.assertThat(read("scripts/setup-secrets.sh")).contains("require_content \"$target\"");
    }
}
