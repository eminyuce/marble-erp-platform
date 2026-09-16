package com.ozerler.marble.service;

import com.ozerler.marble.config.DeploymentProperties;
import com.ozerler.marble.dto.DeploymentLogDto;
import com.ozerler.marble.dto.DeploymentStartResult;
import com.ozerler.marble.dto.DeploymentState;
import com.ozerler.marble.dto.DeploymentStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    static final String PING_TOKEN = "READY";

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
            blockingReasons.add("Yayın yardımcısı henüz kurulmadı. Bir kez SSH ile sudo ./scripts/deploy_production.sh -y çalıştırın.");
        } else if (!sudoReady) {
            blockingReasons.add("Servis kullanıcısının şifresiz sudo yetkisi yok. Bir kez SSH ile sudo ./scripts/deploy_production.sh -y çalıştırın.");
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
            String reason = status.getBlockingReasons().isEmpty()
                    ? "Yayın bu ortamda başlatılamaz."
                    : status.getBlockingReasons().getFirst();
            return DeploymentStartResult.builder()
                    .accepted(false)
                    .state(status.getState())
                    .message(reason)
                    .build();
        }

        List<String> argv = buildWrapperArgv();
        Path repoDir = Path.of(properties.getRepoDir());
        HostCommandRunner.HostCommandResult result = hostCommandRunner.run(
                new HostCommandRunner.HostCommand(argv, repoDir, Duration.ofSeconds(20)));

        if (!result.isSuccess()) {
            log.error("In-app deploy wrapper failed to start. exit={} output={}", result.exitCode(), result.output());
            return DeploymentStartResult.builder()
                    .accepted(false)
                    .state(DeploymentState.FAILED)
                    .message(startFailureMessage(result.output()))
                    .build();
        }

        log.info("In-app production deploy started from the web UI");
        return DeploymentStartResult.builder()
                .accepted(true)
                .state(DeploymentState.RUNNING)
                .message("Yayın başlatıldı. main dalı çekilecek ve üretim paketi kurulacak.")
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
            long start = offset < 0 ? 0 : offset;
            if (start > size) {
                start = 0;
            }
            int chunkSize = Math.min(properties.getLogChunkBytes(), (int) Math.min(Integer.MAX_VALUE, size - start));
            file.seek(start);
            byte[] buffer = new byte[Math.max(chunkSize, 0)];
            int read = chunkSize > 0 ? file.read(buffer) : 0;
            String chunk = read > 0 ? new String(buffer, 0, read, StandardCharsets.UTF_8) : "";
            return DeploymentLogDto.builder()
                    .offset(start)
                    .nextOffset(start + (read < 0 ? 0 : read))
                    .chunk(chunk)
                    .state(fileStatus.state())
                    .startedAt(emptyToNull(fileStatus.startedAt()))
                    .finishedAt(emptyToNull(fileStatus.finishedAt()))
                    .build();
        } catch (IOException ex) {
            log.warn("Could not read deploy log {}: {}", logPath, ex.getMessage());
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
                    Instant.now().plusSeconds(20),
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
            return GitHead.unknown();
        }
        String branch = firstLine(runGit(repoDir, List.of("git", "rev-parse", "--abbrev-ref", "HEAD")));
        String commit = firstLine(runGit(repoDir, List.of("git", "rev-parse", "--short", "HEAD")));
        String message = firstLine(runGit(repoDir, List.of("git", "log", "-1", "--pretty=%s")));
        if (branch.isBlank() && commit.isBlank()) {
            return GitHead.unknown();
        }
        return new GitHead(
                branch.isBlank() ? properties.getGitBranch() : branch,
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
            log.warn("Stale in-app deploy status file at {} (started {})", statusPath, startedAt);
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
            log.warn("Could not read deploy status {}: {}", path, ex.getMessage());
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
        if (fileState == DeploymentState.RUNNING || fileState == DeploymentState.SUCCESS || fileState == DeploymentState.FAILED) {
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
            case IDLE -> canStart
                    ? "main dalından üretim yayını başlatmaya hazır."
                    : "Yayın şu anda başlatılamaz.";
        };
    }

    private static String startFailureMessage(String output) {
        if (output == null || output.isBlank()) {
            return "Yayın süreci başlatılamadı.";
        }
        if (output.toLowerCase(Locale.ROOT).contains("password")) {
            return "sudo şifre istedi. Bir kez SSH ile sudo ./scripts/deploy_production.sh -y çalıştırın.";
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
        return text.lines().map(String::trim).filter(line -> !line.isBlank()).findFirst().orElse("");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
