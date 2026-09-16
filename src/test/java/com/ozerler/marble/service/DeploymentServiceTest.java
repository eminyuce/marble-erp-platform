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
