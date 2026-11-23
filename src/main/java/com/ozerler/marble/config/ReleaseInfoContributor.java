package com.ozerler.marble.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Publishes the pom.xml version and runtime facts on {@code /actuator/info}.
 * The deployment console prints these fields. No secrets are included.
 */
@Component
public class ReleaseInfoContributor implements InfoContributor {

    static final String SESSION_STORE = "JDBC";

    private final Optional<BuildProperties> buildProperties;
    private final Environment environment;
    private final ObjectStorageProperties storageProperties;
    private final DeploymentProperties deploymentProperties;

    public ReleaseInfoContributor(
            Optional<BuildProperties> buildProperties,
            Environment environment,
            ObjectStorageProperties storageProperties,
            DeploymentProperties deploymentProperties) {
        this.buildProperties = buildProperties;
        this.environment = environment;
        this.storageProperties = storageProperties;
        this.deploymentProperties = deploymentProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> release = new LinkedHashMap<>();
        release.put("appName", buildProperties.map(props -> props.getName()).orElse("Özerler Mermer ERP"));
        release.put("appVersion", buildProperties.map(props -> props.getVersion()).orElse("unknown"));
        release.put("buildTime", buildTime());
        release.put("gitCommit", gitCommit(System.getenv("APP_GIT_SHA"), gitProperties()));
        release.put("gitBranch", blankToUnknown(deploymentProperties.getGitBranch()));
        release.put("springBootVersion", blankToUnknown(SpringBootVersion.getVersion()));
        release.put("springFrameworkVersion", blankToUnknown(SpringVersion.getVersion()));
        release.put("javaVersion", Runtime.version().toString());
        release.put("activeProfiles", activeProfiles());
        release.put("storageType", blankToUnknown(storageProperties.getType()));
        release.put("bucket", blankToUnknown(storageProperties.getRustfs().getBucket()));
        release.put("sessionStore", SESSION_STORE);
        release.put("image", deploymentProperties.getImageRepository() + ":" + deploymentProperties.getImageTag());
        builder.withDetail("release", release);
    }

    private String buildTime() {
        return buildProperties
                .map(props -> props.getTime())
                .map(time -> time.toString())
                .orElse("unknown");
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            return "default";
        }
        return String.join(",", profiles);
    }

    static String gitCommit(String envValue, Properties file) {
        String fromEnv = shortSha(envValue);
        if (!"unknown".equals(fromEnv)) {
            return fromEnv;
        }
        if (file == null) {
            return "unknown";
        }
        return shortSha(file.getProperty("git.commit.id"));
    }

    private static Properties gitProperties() {
        try (InputStream in = ReleaseInfoContributor.class.getResourceAsStream("/git.properties")) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException ex) {
            return null;
        }
    }

    static String shortSha(String sha) {
        if (sha == null || sha.isBlank() || "unknown".equalsIgnoreCase(sha.trim())) {
            return "unknown";
        }
        String trimmed = sha.trim();
        if (trimmed.length() <= 12) {
            return trimmed;
        }
        return trimmed.substring(0, 12);
    }

    private static String blankToUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}
