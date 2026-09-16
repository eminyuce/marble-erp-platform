package com.ozerler.marble.service;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Runs host processes. Isolated so unit tests never spawn real shells.
 */
public interface HostCommandRunner {

    HostCommandResult run(HostCommand command);

    record HostCommand(
            List<String> argv,
            Path workingDirectory,
            Duration timeout,
            Map<String, String> extraEnv
    ) {
        public HostCommand(List<String> argv, Path workingDirectory, Duration timeout) {
            this(argv, workingDirectory, timeout, Map.of());
        }
    }

    record HostCommandResult(int exitCode, String output) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
