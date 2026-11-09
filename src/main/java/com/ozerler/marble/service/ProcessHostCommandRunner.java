package com.ozerler.marble.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ProcessHostCommandRunner implements HostCommandRunner {

    @Override
    public HostCommandResult run(HostCommand command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command.argv());
        processBuilder.redirectErrorStream(true);
        if (command.workingDirectory() != null && Files.isDirectory(command.workingDirectory())) {
            processBuilder.directory(command.workingDirectory().toFile());
        }
        if (command.extraEnv() != null && !command.extraEnv().isEmpty()) {
            processBuilder.environment().putAll(command.extraEnv());
        }

        Process process = null;
        try {
            process = processBuilder.start();
            InputStream stream = process.getInputStream();
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
                } catch (IOException ex) {
                    return "";
                }
            });
            long timeoutMillis = command.timeout() != null ? command.timeout().toMillis() : 15_000L;
            boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Host command timed out: {}", command.argv());
                return new HostCommandResult(-1, outputFuture.getNow(""));
            }
            String output = outputFuture.get(2, TimeUnit.SECONDS);
            return new HostCommandResult(process.exitValue(), output);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Host command interrupted: {}", command.argv());
            return new HostCommandResult(-1, "interrupted");
        } catch (IOException ex) {
            log.warn("Host command failed: {} — {}", command.argv(), ex.getMessage());
            return new HostCommandResult(-1, ex.getMessage() == null ? "" : ex.getMessage());
        } catch (Exception ex) {
            log.warn("Host command output read failed: {} — {}", command.argv(), ex.getMessage());
            return new HostCommandResult(process != null && !process.isAlive() ? process.exitValue() : -1, "");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
