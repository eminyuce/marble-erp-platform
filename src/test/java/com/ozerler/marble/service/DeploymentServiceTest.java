package com.ozerler.marble.service;

import com.ozerler.marble.config.DeploymentProperties;
import com.ozerler.marble.dto.DeploymentLogDto;
import com.ozerler.marble.dto.DeploymentStartResult;
import com.ozerler.marble.dto.DeploymentState;
import com.ozerler.marble.dto.DeploymentStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentServiceTest {

    @TempDir
    Path tempDir;

    private Path commandFile;
    private Path repoDir;
    private Path logFile;
    private Path statusFile;
    private RecordingCommandRunner commandRunner;
    private DeploymentProperties properties;
    private DeploymentService deploymentService;

    @BeforeEach
    void setUp() throws Exception {
        commandFile = tempDir.resolve("marble-erp-inapp-deploy");
        Files.writeString(commandFile, "#!/bin/sh\necho READY\n", StandardCharsets.UTF_8);
        repoDir = tempDir.resolve("repo");
        Files.createDirectories(repoDir.resolve(".git"));
        logFile = tempDir.resolve("inapp-deploy.log");
        statusFile = tempDir.resolve("inapp-deploy.status");

        properties = new DeploymentProperties();
        properties.setEnabled(true);
        properties.setCommand(commandFile.toString());
        properties.setRepoDir(repoDir.toString());
        properties.setLogFile(logFile.toString());
        properties.setStatusFile(statusFile.toString());
        properties.setLockFile(tempDir.resolve("inapp-deploy.lock").toString());
        properties.setUseSudo(false);

        commandRunner = new RecordingCommandRunner();
        deploymentService = new DeploymentService(properties, commandRunner);
    }

    @Test
    @DisplayName("currentStatus refuses to start when the feature is disabled")
    void currentStatus_Disabled_CannotStart() {
        properties.setEnabled(false);

        DeploymentStatusDto status = deploymentService.currentStatus();

        assertThat(status.isCanStart()).isFalse();
        assertThat(status.getBlockingReasons()).anyMatch(reason -> reason.contains("kapalı"));
    }

    @Test
    @DisplayName("currentStatus refuses to start when the wrapper binary is missing")
    void currentStatus_MissingCommand_CannotStart() throws Exception {
        Files.deleteIfExists(commandFile);

        DeploymentStatusDto status = deploymentService.currentStatus();

        assertThat(status.isCanStart()).isFalse();
        assertThat(status.getBlockingReasons()).anyMatch(reason -> reason.contains("yardımcısı"));
        assertThat(status.isCommandAvailable()).isFalse();
    }

    @Test
    @DisplayName("startDeploy launches the wrapper and reports accepted when probes pass")
    void startDeploy_WhenReady_StartsWrapper() {
        commandRunner.pingOutput = DeploymentService.PING_TOKEN;
        commandRunner.startExitCode = 0;
        DeploymentService linuxService = new DeploymentService(properties, commandRunner) {
            @Override
            boolean isLinuxHost() {
                return true;
            }
        };

        DeploymentStartResult result = linuxService.startDeploy();

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getState()).isEqualTo(DeploymentState.RUNNING);
        assertThat(commandRunner.startedCommands).isNotEmpty();
        assertThat(commandRunner.startedCommands.getLast()).contains(commandFile.toString());
        assertThat(commandRunner.startedCommands.getLast()).doesNotContain("sudo");
    }

    @Test
    @DisplayName("currentStatus on a non-Linux host cannot start a production deploy")
    void currentStatus_NonLinux_CannotStart() {
        properties.setEnabled(true);
        DeploymentService windowsService = new DeploymentService(properties, commandRunner) {
            @Override
            boolean isLinuxHost() {
                return false;
            }
        };

        DeploymentStatusDto status = windowsService.currentStatus();

        assertThat(status.isCanStart()).isFalse();
        assertThat(status.isLinuxHost()).isFalse();
        assertThat(status.getImageRepository()).isEqualTo("guvenulu/marble-erp-platform");
        assertThat(status.getImageTag()).isEqualTo("latest");
        assertThat(status.getBlockingReasons()).anyMatch(reason -> reason.contains("Linux"));
    }

    @Test
    @DisplayName("startDeploy is rejected while a RUNNING status file exists")
    void startDeploy_AlreadyRunning_Rejected() throws Exception {
        Files.writeString(statusFile, """
                STATE=RUNNING
                STARTED_AT=%s
                FINISHED_AT=
                EXIT_CODE=
                """.formatted(Instant.now().toString()), StandardCharsets.UTF_8);

        DeploymentStartResult result = deploymentService.startDeploy();

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.getState()).isEqualTo(DeploymentState.RUNNING);
        assertThat(commandRunner.startedCommands)
                .noneMatch(argv -> argv.contains(commandFile.toString()) && !argv.contains("--ping"));
    }

    @Test
    @DisplayName("readLog returns incremental chunks from the deploy log file")
    void readLog_ReturnsChunksFromOffset() throws Exception {
        Files.writeString(logFile, "hello\nworld\n", StandardCharsets.UTF_8);

        DeploymentLogDto first = deploymentService.readLog(0);
        assertThat(first.getChunk()).isEqualTo("hello\nworld\n");
        assertThat(first.getNextOffset()).isEqualTo("hello\nworld\n".getBytes(StandardCharsets.UTF_8).length);

        DeploymentLogDto second = deploymentService.readLog(first.getNextOffset());
        assertThat(second.getChunk()).isEmpty();
        assertThat(second.getNextOffset()).isEqualTo(first.getNextOffset());
    }

    @Test
    @DisplayName("stale RUNNING status older than the timeout is treated as FAILED")
    void currentStatus_StaleRunning_BecomesFailed() throws Exception {
        properties.setStaleRunningMinutes(1);
        Files.writeString(statusFile, """
                STATE=RUNNING
                STARTED_AT=%s
                FINISHED_AT=
                EXIT_CODE=
                """.formatted(Instant.now().minus(Duration.ofHours(2)).toString()), StandardCharsets.UTF_8);

        DeploymentStatusDto status = deploymentService.currentStatus();

        assertThat(status.getState()).isEqualTo(DeploymentState.FAILED);
    }

    @Test
    @DisplayName("docker-compose.prod.yml avoids /stack file bind mounts and fixes volume permissions recursively")
    void prodCompose_AvoidsStackFileMountsAndFixesVolumePerms() throws Exception {
        String compose = Files.readString(Path.of("docker-compose.prod.yml"), StandardCharsets.UTF_8);

        assertThat(compose).doesNotContain("./docker-compose.prod.yml:/stack/docker-compose.prod.yml");
        assertThat(compose).doesNotContain("./.env:/stack/.env");
        assertThat(compose).contains("chown -R 1001:1001 /logs /uploads /media");
    }

    @Test
    @DisplayName("deploy_from_app.sh syncs compose file from pulled image and marks status FAILED if docker run fails")
    void deployFromAppScript_SyncsComposeFileAndHandlesLaunchFailure() throws Exception {
        String script = Files.readString(Path.of("scripts/deploy_from_app.sh"), StandardCharsets.UTF_8);

        assertThat(script).contains("-v \"${host_stack}:${host_stack}\"");
        assertThat(script).contains("docker cp \"\\$cid:/stack/docker-compose.prod.yml\"");
        assertThat(script).contains("write_status \"FAILED\" \"$started_at\"");
        assertThat(script).contains("chown -R 1001:1001 /logs");
    }

    @Test
    @DisplayName("deploy_from_app.sh performs app-only update with --no-deps app, deep health check, and automatic rollback")
    void deployFromAppScript_UsesAppOnlyUpdateAndAutomaticRollback() throws Exception {
        String script = Files.readString(Path.of("scripts/deploy_from_app.sh"), StandardCharsets.UTF_8);

        assertThat(script).contains("pull app");
        assertThat(script).contains("up -d --no-deps app");
        assertThat(script).contains("rollback-previous");
        assertThat(script).contains("last-known-good.env");
        assertThat(script).contains("/health/");
        assertThat(script).doesNotContain("down -v");
        assertThat(script).doesNotContain("volume rm");
        assertThat(script).doesNotContain("volume prune");
    }

    @Test
    @DisplayName("Dockerfile and .dockerignore bundle compose files, .env.example, scripts, and docker-compose v2 plugin")
    void dockerfileAndDockerignore_BundleStackAndComposePlugin() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"), StandardCharsets.UTF_8);
        String dockerignore = Files.readString(Path.of(".dockerignore"), StandardCharsets.UTF_8);

        assertThat(dockerignore).contains("!.env.example");
        assertThat(dockerfile).contains("/usr/local/lib/docker/cli-plugins/docker-compose");
        assertThat(dockerfile).contains("COPY docker-compose.prod.yml /stack/docker-compose.prod.yml");
        assertThat(dockerfile).contains("COPY docker/docker-compose.yml /stack/docker-compose.yml");
        assertThat(dockerfile).contains("COPY .env.example /stack/.env.example");
        assertThat(dockerfile).contains("ENTRYPOINT [\"/usr/local/bin/docker-entrypoint.sh\"]");
    }

    @Test
    @DisplayName("Production compose, CI/CD pipelines, and operational scripts enforce zero-data-loss rules")
    void productionInfrastructure_EnforcesZeroDataLossRules() throws Exception {
        String prodCompose = Files.readString(Path.of("docker-compose.prod.yml"), StandardCharsets.UTF_8);
        String githubWorkflow = Files.readString(Path.of(".github/workflows/docker-publish.yml"), StandardCharsets.UTF_8);
        String gitlabCi = Files.readString(Path.of(".gitlab-ci.yml"), StandardCharsets.UTF_8);
        String backupScript = Files.readString(Path.of("scripts/backup_production.sh"), StandardCharsets.UTF_8);
        String rollbackScript = Files.readString(Path.of("scripts/rollback_production.sh"), StandardCharsets.UTF_8);
        String freshInstallScript = Files.readString(Path.of("scripts/init_fresh_install.sh"), StandardCharsets.UTF_8);

        assertThat(prodCompose).contains("name: ${POSTGRES_VOLUME_NAME:-docker_postgres_data}");
        assertThat(prodCompose).contains("external: true");
        assertThat(githubWorkflow).contains("type=sha");
        assertThat(githubWorkflow).contains("GIT_COMMIT_SHA=${{ github.sha }}");
        assertThat(gitlabCi).contains("./scripts/deploy_from_app.sh --foreground");
        assertThat(backupScript).contains("-Fc");
        assertThat(backupScript).contains("pg_restore --list");
        assertThat(rollbackScript).contains("up -d --no-deps app");
        assertThat(freshInstallScript).contains("Existing production installation detected");
    }

    private static final class RecordingCommandRunner implements HostCommandRunner {
        private final List<List<String>> startedCommands = new ArrayList<>();
        private String pingOutput = DeploymentService.PING_TOKEN;
        private int startExitCode = 0;
        private final AtomicInteger gitCalls = new AtomicInteger();

        @Override
        public HostCommandResult run(HostCommand command) {
            List<String> argv = command.argv();
            startedCommands.add(List.copyOf(argv));
            if (argv.contains("--ping")) {
                return new HostCommandResult(0, pingOutput);
            }
            if (!argv.isEmpty() && "git".equals(argv.getFirst())) {
                gitCalls.incrementAndGet();
                if (argv.contains("--abbrev-ref")) {
                    return new HostCommandResult(0, "main");
                }
                if (argv.contains("--short")) {
                    return new HostCommandResult(0, "abc1234");
                }
                return new HostCommandResult(0, "latest commit");
            }
            return new HostCommandResult(startExitCode, startExitCode == 0 ? "started" : "failed");
        }
    }
}
