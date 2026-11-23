package com.ozerler.marble.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Copies {@code *_FILE} environment variables into the matching setting.
 * A trailing newline written by editors and shell redirection is removed once.
 * The secret value is never logged.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecretFileEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "secretFiles";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> loaded = new HashMap<>();
        for (Map.Entry<String, Object> entry : environment.getSystemEnvironment().entrySet()) {
            String name = entry.getKey();
            if (name == null || !name.endsWith("_FILE")) {
                continue;
            }
            String configuredPath;
            if (entry.getValue() == null) {
                configuredPath = "";
            } else {
                configuredPath = String.valueOf(entry.getValue());
            }
            if (configuredPath.isBlank()) {
                throw new IllegalStateException(name + " is set but empty");
            }
            String secret = readSecret(name, Path.of(configuredPath));
            loaded.put(name.substring(0, name.length() - "_FILE".length()), secret);
        }
        if (!loaded.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, loaded));
        }
    }

    static String readSecret(String variableName, Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(variableName + " does not point to a readable file");
        }
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(variableName + " could not be read", exception);
        }
        if (text.endsWith("\r\n")) {
            text = text.substring(0, text.length() - 2);
        } else if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.isBlank()) {
            throw new IllegalStateException(variableName + " is empty");
        }
        return text;
    }
}
