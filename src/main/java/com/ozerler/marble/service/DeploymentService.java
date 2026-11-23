package com.ozerler.marble.service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ozerler.marble.config.DeploymentProperties;
import com.ozerler.marble.dto.DeploymentLogDto;
import com.ozerler.marble.dto.DeploymentStartResult;
import com.ozerler.marble.dto.DeploymentState;
import com.ozerler.marble.dto.DeploymentStatusDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentService {

    static final String PING_TOKEN = "READY";
    private static final int COMMAND_TIMEOUT_SECONDS = 60;
    private static final int PROBE_CACHE_SECONDS = 20;

    private final DeploymentProperties properties;
    private final HostCommandRunner hostCommandRunner;
    private final Object probeLock = new Object();
    private volatile ProbeSnapshot probeSnapshot;

    public DeploymentStatusDto currentStatus() {
        List<String> blockingReasons = new ArrayList<>();
        boolean linuxHost = isLinuxHost();
        boolean enabled = properties.isEnabled();
        ProbeSnapshot probe = refreshProbeSnapshot();
        boolean commandAvailable = probe.commandAvailable();
        boolean sudoReady = probe.sudoReady();
        GitHead gitHead = probe.gitHead();
        DeployFileStatus fileStatus = readStatusFile();

        if (!enabled) {
            blockingReasons.add("Üretim yayın ekranı bu ortamda kapalı (yalnızca Linux prod).");
        }
        if (!linuxHost) {
            blockingReasons.add("Bu makina Linux üretim sunucusu değil.");
        }
        if (!commandAvailable) {
            blockingReasons.add("Yayın yardımcısı henüz yok. İmajın içinde marble-erp-inapp-deploy olmalı.");
        } else if (!sudoReady) {
            blockingReasons.add(
                    "Yayın yardımcısı çalıştırılamadı. Konteynerde sudo kapalı olmalı, sunucuda şifresiz sudo "
                            + "gerekir.");
        }
        if (fileStatus.state() == DeploymentState.RUNNING) {
            blockingReasons.add("Bir yayın işlemi zaten sürüyor.");
        }

        boolean canStart = enabled && linuxHost && commandAvailable && sudoReady
                && fileStatus.state() != DeploymentState.RUNNING;

        DeploymentState state = resolvePublicState(enabled, linuxHost, commandAvailable, sudoReady, fileStatus.state());
        return DeploymentStatusDto.builder()
                .state(state)
                .canStart(canStart)
                .linuxHost(linuxHost)
                .enabled(enabled)
                .commandAvailable(commandAvailable)
                .sudoReady(sudoReady)
                .branch(gitHead.branch())
                .headCommit(gitHead.commit())
                .headMessage(gitHead.message())
                .imageRepository(properties.getImageRepository())
                .imageTag(properties.getImageTag())
                .startedAt(emptyToNull(fileStatus.startedAt()))
                .finishedAt(emptyToNull(fileStatus.finishedAt()))
                .message(statusMessage(state, canStart))
                .blockingReasons(List.copyOf(blockingReasons))
                .build();
    }

    public DeploymentStartResult startDeploy() {
        DeploymentStatusDto status = currentStatus();
        if (status.getState() == DeploymentState.RUNNING) {
            return DeploymentStartResult.builder()
                    .accepted(false)
                    .state(DeploymentState.RUNNING)
                    .message("Bir yayın işlemi zaten sürüyor.")
                    .build();
        }
        if (!status.isCanStart()) {
            String reason;
            if (status.getBlockingReasons().isEmpty()) {
                reason = "Yayın bu ortamda başlatılamaz.";
            } else {
                reason = status.getBlockingReasons().getFirst();
            }
            return DeploymentStartResult.builder()
                    .accepted(false)
                    .state(status.getState())
                    .message(reason)
                    .build();
        }

        List<String> argv = buildWrapperArgv();
        Path repoDir = Path.of(properties.getRepoDir());
        HostCommandRunner.HostCommandResult result = hostCommandRunner.run(
                new HostCommandRunner.HostCommand(argv, repoDir, Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS)));

        if (!result.isSuccess()) {
            return DeploymentStartResult.builder()
                    .accepted(false)
                    .state(DeploymentState.FAILED)
                    .message(startFailureMessage(result.output()))
                    .build();
        }

        return DeploymentStartResult.builder()
                .accepted(true)
                .state(DeploymentState.RUNNING)
                .message("Yayın başlatıldı. Docker Hub imajı indirilip uygulama konteyneri yenilenecek.")
                .build();
    }

    public DeploymentLogDto readLog(long offset) {
        DeployFileStatus fileStatus = readStatusFile();
        Path logPath = Path.of(properties.getLogFile());
        if (!Files.isRegularFile(logPath)) {
            return DeploymentLogDto.builder()
                    .offset(Math.max(0, offset))
                    .nextOffset(Math.max(0, offset))
                    .chunk("")
                    .state(fileStatus.state())
                    .startedAt(emptyToNull(fileStatus.startedAt()))
                    .finishedAt(emptyToNull(fileStatus.finishedAt()))
                    .build();
        }

        try (RandomAccessFile file = new RandomAccessFile(logPath.toFile(), "r")) {
            long size = file.length();
            long start;
            if (offset < 0) {
                start = 0;
            } else {
                start = offset;
            }
            if (start > size) {
                start = 0;
            }
            int chunkSize = Math.min(properties.getLogChunkBytes(), (int) Math.min(Integer.MAX_VALUE, size - start));
            file.seek(start);
            byte[] buffer = new byte[Math.max(chunkSize, 0)];
            int read;
            if (chunkSize > 0) {
                read = file.read(buffer);
            } else {
                read = 0;
            }
            String chunk;
            if (read > 0) {
                chunk = new String(buffer, 0, read, StandardCharsets.UTF_8);
            } else {
                chunk = "";
            }
            int value;
            if (read < 0) {
                value = 0;
            } else {
                value = read;
            }
            return DeploymentLogDto.builder()
                    .offset(start)
                    .nextOffset(start + value)
                    .chunk(chunk)
                    .state(fileStatus.state())
                    .startedAt(emptyToNull(fileStatus.startedAt()))
                    .finishedAt(emptyToNull(fileStatus.finishedAt()))
                    .build();
        } catch (IOException ex) {
            return DeploymentLogDto.builder()
                    .offset(Math.max(0, offset))
                    .nextOffset(Math.max(0, offset))
                    .chunk("")
                    .state(fileStatus.state())
                    .startedAt(emptyToNull(fileStatus.startedAt()))
                    .finishedAt(emptyToNull(fileStatus.finishedAt()))
                    .build();
        }
    }

    private List<String> buildWrapperArgv() {
        List<String> argv = new ArrayList<>();
        if (properties.isUseSudo()) {
            argv.add("sudo");
            argv.add("-n");
        }
        argv.add(properties.getCommand());
        argv.add("--repo");
        argv.add(properties.getRepoDir());
        argv.add("--log");
        argv.add(properties.getLogFile());
        argv.add("--status");
        argv.add(properties.getStatusFile());
        argv.add("--lock");
        argv.add(properties.getLockFile());
        argv.add("--remote");
        argv.add(properties.getGitRemote());
        argv.add("--branch");
        argv.add(properties.getGitBranch());
        return argv;
    }

    private ProbeSnapshot refreshProbeSnapshot() {
        ProbeSnapshot cached = probeSnapshot;
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached;
        }
        synchronized (probeLock) {
            cached = probeSnapshot;
            if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
                return cached;
            }
            boolean commandAvailable = Files.isRegularFile(commandPath());
            boolean sudoReady = commandAvailable && pingWrapper();
            GitHead gitHead = readGitHead();
            ProbeSnapshot fresh = new ProbeSnapshot(
                    Instant.now().plusSeconds(PROBE_CACHE_SECONDS),
                    commandAvailable,
                    sudoReady,
                    gitHead
            );
            probeSnapshot = fresh;
            return fresh;
        }
    }

    private boolean pingWrapper() {
        List<String> argv = new ArrayList<>();
        if (properties.isUseSudo()) {
            argv.add("sudo");
            argv.add("-n");
        }
        argv.add(properties.getCommand());
        argv.add("--ping");
        HostCommandRunner.HostCommandResult result = hostCommandRunner.run(
                new HostCommandRunner.HostCommand(argv, Path.of(properties.getRepoDir()), Duration.ofSeconds(8)));
        return result.isSuccess() && result.output().contains(PING_TOKEN);
    }

    private GitHead readGitHead() {
        Path repoDir = Path.of(properties.getRepoDir());
        if (!Files.isDirectory(repoDir.resolve(".git"))) {
            String envSha = System.getenv("APP_GIT_SHA");
            String envVersion = System.getenv("APP_VERSION");
            if (envSha != null && !envSha.isBlank() && !"unknown".equalsIgnoreCase(envSha)) {
                String shortSha;
                if (envSha.length() > 7) {
                    shortSha = envSha.substring(0, 7);
                } else {
                    shortSha = envSha;
                }
                String msg;
                if (envVersion != null && !envVersion.isBlank()) {
                    msg = "v" + envVersion;
                } else {
                    msg = "docker-image";
                }
                return new GitHead(properties.getGitBranch(), shortSha, msg);
            }
            return GitHead.unknown();
        }
        String branch = firstLine(runGit(repoDir, List.of("git", "rev-parse", "--abbrev-ref", "HEAD")));
        String commit = firstLine(runGit(repoDir, List.of("git", "rev-parse", "--short", "HEAD")));
        String message = firstLine(runGit(repoDir, List.of("git", "log", "-1", "--pretty=%s")));
        if (branch.isBlank() && commit.isBlank()) {
            return GitHead.unknown();
        }
        String gitBranch;
        if (branch.isBlank()) {
            gitBranch = properties.getGitBranch();
        } else {
            gitBranch = branch;
        }
        return new GitHead(
                gitBranch,
                commit,
                message
        );
    }

    private String runGit(Path repoDir, List<String> argv) {
        return hostCommandRunner.run(new HostCommandRunner.HostCommand(argv, repoDir, Duration.ofSeconds(8))).output();
    }

    private DeployFileStatus readStatusFile() {
        Path statusPath = Path.of(properties.getStatusFile());
        if (!Files.isRegularFile(statusPath)) {
            return DeployFileStatus.idle();
        }
        Map<String, String> values = readKeyValues(statusPath);
        DeploymentState state = parseState(values.get("STATE"));
        String startedAt = values.getOrDefault("STARTED_AT", "");
        if (state == DeploymentState.RUNNING && isStale(startedAt)) {
            return new DeployFileStatus(DeploymentState.FAILED, startedAt, values.getOrDefault("FINISHED_AT", ""),
                    values.getOrDefault("EXIT_CODE", ""));
        }
        return new DeployFileStatus(state, startedAt, values.getOrDefault("FINISHED_AT", ""),
                values.getOrDefault("EXIT_CODE", ""));
    }

    private boolean isStale(String startedAt) {
        if (startedAt == null || startedAt.isBlank()) {
            return false;
        }
        try {
            Instant started = Instant.parse(startedAt);
            return Instant.now().isAfter(started.plus(Duration.ofMinutes(properties.getStaleRunningMinutes())));
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static Map<String, String> readKeyValues(Path path) {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                values.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        } catch (IOException ex) {
            log.debug("Deploy status file could not be read: {}", path, ex);
        }
        return values;
    }

    private static DeploymentState parseState(String raw) {
        if (raw == null || raw.isBlank()) {
            return DeploymentState.IDLE;
        }
        try {
            return DeploymentState.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DeploymentState.IDLE;
        }
    }

    private static DeploymentState resolvePublicState(boolean enabled, boolean linuxHost, boolean commandAvailable,
                                                      boolean sudoReady, DeploymentState fileState) {
        if (fileState == DeploymentState.RUNNING
                || fileState == DeploymentState.SUCCESS
                || fileState == DeploymentState.FAILED) {
            return fileState;
        }
        if (!enabled || !linuxHost || !commandAvailable || !sudoReady) {
            return DeploymentState.UNAVAILABLE;
        }
        return DeploymentState.IDLE;
    }

    private static String statusMessage(DeploymentState state, boolean canStart) {
        return switch (state) {
            case RUNNING -> "Yayın sürüyor. Sunucu kısa süre yanıt vermeyebilir.";
            case SUCCESS -> "Son yayın tamamlandı.";
            case FAILED -> "Son yayın başarısız oldu. Günlüğü inceleyin.";
            case UNAVAILABLE -> "Bu ortamdan yayın başlatılamaz.";
            case IDLE -> idleMessage(canStart);
        };
    }

    private static String idleMessage(boolean canStart) {
        if (canStart) {
            return "Docker Hub imajını çekip yayına almaya hazır.";
        }
        return "Yayın şu anda başlatılamaz.";
    }

    private static String startFailureMessage(String output) {
        if (output == null || output.isBlank()) {
            return "Yayın süreci başlatılamadı.";
        }
        if (output.toLowerCase(Locale.ROOT).contains("password")) {
            return "Yayın yardımcısı şifre istedi. Konteynerde sudo kapalı olmalı.";
        }
        return output.lines().findFirst().orElse("Yayın süreci başlatılamadı.");
    }

    private Path commandPath() {
        return Path.of(properties.getCommand());
    }

    boolean isLinuxHost() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).contains("linux");
    }

    private static String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.lines().map(line -> line.trim()).filter(line -> !line.isBlank()).findFirst().orElse("");
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record ProbeSnapshot(Instant expiresAt, boolean commandAvailable, boolean sudoReady, GitHead gitHead) {
    }

    private record GitHead(String branch, String commit, String message) {
        static GitHead unknown() {
            return new GitHead("", "", "");
        }
    }

    private record DeployFileStatus(DeploymentState state, String startedAt, String finishedAt, String exitCode) {
        static DeployFileStatus idle() {
            return new DeployFileStatus(DeploymentState.IDLE, "", "", "");
        }
    }
}
