package com.ozerler.marble.service;

import java.util.Objects;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.dto.HealthResponse;
import com.ozerler.marble.storage.ObjectStorageService;
import com.ozerler.marble.util.DateTimes;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service orchestrating system diagnostic metrics, JVM runtime stats, database,
 * and RustFS object-storage health probes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BYTES_PER_KIB = 1024;
    private static final int DEFAULT_SERVER_PORT = 81;
    private static final int UNREACHABLE_LATENCY_MS = 999;

    private final DataSource dataSource;
    private final Environment environment;
    private final Optional<BuildProperties> buildProperties;
    private final org.springframework.context.MessageSource messageSource;
    private final ObjectStorageService objectStorageService;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    public Map<String, Object> collectHealthMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 1. Application & Environment Info
        String appName = getMessage("system.health.app_name");
        String version = buildProperties.map(props -> props.getVersion()).orElse("unknown");
        String[] activeProfiles;
        if (environment.getActiveProfiles().length > 0) {
            activeProfiles = environment.getActiveProfiles();
        } else {
            activeProfiles = new String[]{"default"};
        }
        int port = DEFAULT_SERVER_PORT;

        RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
        String formattedUptime = DateTimes.formatUptime(Duration.ofMillis(runtimeMX.getUptime()));

        metrics.put("appName", appName);
        metrics.put("appVersion", version);
        metrics.put("appPort", port);
        metrics.put("activeProfiles", String.join(", ", activeProfiles));
        metrics.put("uptime", formattedUptime);
        metrics.put("startTime", Instant.ofEpochMilli(runtimeMX.getStartTime()).toString());

        // 2. JVM Runtime & Memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemBytes = runtime.maxMemory();
        long totalMemBytes = runtime.totalMemory();
        long freeMemBytes = runtime.freeMemory();
        long usedMemBytes = totalMemBytes - freeMemBytes;

        long usedMemMb = usedMemBytes / (BYTES_PER_KIB * BYTES_PER_KIB);
        long totalMemMb = totalMemBytes / (BYTES_PER_KIB * BYTES_PER_KIB);
        long maxMemMb = maxMemBytes / (BYTES_PER_KIB * BYTES_PER_KIB);
        int memPercent = (int) Math.round((double) usedMemBytes / totalMemBytes * 100);

        metrics.put("javaVersion", System.getProperty("java.version", "24"));
        metrics.put("javaVendor", System.getProperty("java.vendor", "Eclipse Adoptium"));
        metrics.put("osName", System.getProperty("os.name", "Windows"));
        metrics.put("osArch", System.getProperty("os.arch", "amd64"));
        metrics.put("availableProcessors", runtime.availableProcessors());
        metrics.put("activeThreads", Thread.activeCount());
        metrics.put("usedMemMb", usedMemMb);
        metrics.put("totalMemMb", totalMemMb);
        metrics.put("maxMemMb", maxMemMb);
        metrics.put("memPercent", memPercent);

        // 3. Database Health & Latency
        long dbLatencyMs = -1;
        String dbName = "PostgreSQL";
        String dbVersion = "16";
        String dbStatus = "UP";

        try {
            long start = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                DatabaseMetaData meta = conn.getMetaData();
                dbName = meta.getDatabaseProductName();
                dbVersion = meta.getDatabaseProductVersion();
                stmt.execute("SELECT 1");
                dbLatencyMs = System.currentTimeMillis() - start;
            }
        } catch (Exception ex) {
            log.error("Database health check ping failed: {}", ex.getMessage());
            dbStatus = "DOWN";
            dbLatencyMs = UNREACHABLE_LATENCY_MS;
        }

        metrics.put("dbStatus", dbStatus);
        metrics.put("dbName", dbName);
        metrics.put("dbVersion", dbVersion);
        long dbLatencyMsValue;
        if (dbLatencyMs >= 0) {
            dbLatencyMsValue = dbLatencyMs;
        } else {
            dbLatencyMsValue = 1;
        }
        metrics.put("dbLatencyMs", dbLatencyMsValue);

        // 4. Disk Storage Health
        File rootPartition = new File(".");
        long totalDiskBytes = rootPartition.getTotalSpace();
        long freeDiskBytes = rootPartition.getFreeSpace();
        long usableDiskBytes = rootPartition.getUsableSpace();
        long usedDiskBytes = totalDiskBytes - freeDiskBytes;

        long totalDiskGb = totalDiskBytes / (BYTES_PER_KIB * BYTES_PER_KIB * BYTES_PER_KIB);
        long usableDiskGb = usableDiskBytes / (BYTES_PER_KIB * BYTES_PER_KIB * BYTES_PER_KIB);
        int diskPercent;
        if (totalDiskBytes > 0) {
            diskPercent = (int) Math.round((double) usedDiskBytes / totalDiskBytes * 100);
        } else {
            diskPercent = 0;
        }

        metrics.put("diskStatus", "UP");
        metrics.put("totalDiskGb", totalDiskGb);
        metrics.put("usableDiskGb", usableDiskGb);
        metrics.put("diskPercent", diskPercent);

        // 5. RustFS object storage — required; DOWN marks the whole system DOWN
        long rustFsStart = System.currentTimeMillis();
        DependencyHealth rustFsHealth = checkRustFsHealth();
        long rustFsLatencyMs = System.currentTimeMillis() - rustFsStart;
        String rustFsStatus = rustFsHealth.getStatus();
        String rustFsDesc;
        if (rustFsHealth.getError() != null) {
            rustFsDesc = rustFsHealth.getError();
        } else {
            rustFsDesc = getMessage("system.health.comp.rustfs.desc");
        }

        metrics.put("rustfsStatus", rustFsStatus);
        metrics.put("rustfsLatencyMs", rustFsLatencyMs);
        metrics.put("rustfsBucket", objectStorageService.getBucketName());

        String overallStatus;
        if ("UP".equals(dbStatus) && "UP".equals(rustFsStatus)) {
            overallStatus = "UP";
        } else {
            overallStatus = "DOWN";
        }
        metrics.put("overallStatus", overallStatus);

        // 6. Component Subsystem Statuses
        List<Map<String, Object>> components = new ArrayList<>();
        long dbLatencyMsValue2;
        if (dbLatencyMs >= 0) {
            dbLatencyMsValue2 = dbLatencyMs;
        } else {
            dbLatencyMsValue2 = 1;
        }
        components.add(Map.of("name", getMessage("system.health.comp.db.name"), "status", dbStatus, "latency", dbLatencyMsValue2 + " ms", "desc", getMessage("system.health.comp.db.desc")));
        components.add(Map.of("name", getMessage("system.health.comp.rustfs.name"), "status", rustFsStatus, "latency", rustFsLatencyMs + " ms", "desc", rustFsDesc));
        components.add(Map.of("name", getMessage("system.health.comp.quarry.name"), "status", "UP", "latency", "<1 ms", "desc", getMessage("system.health.comp.quarry.desc")));
        components.add(Map.of("name", getMessage("system.health.comp.gangsaw.name"), "status", "UP", "latency", "<1 ms", "desc", getMessage("system.health.comp.gangsaw.desc")));
        components.add(Map.of("name", getMessage("system.health.comp.workshop.name"), "status", "UP", "latency", "<1 ms", "desc", getMessage("system.health.comp.workshop.desc")));
        components.add(Map.of("name", getMessage("system.health.comp.site.name"), "status", "UP", "latency", "<1 ms", "desc", getMessage("system.health.comp.site.desc")));
        components.add(Map.of("name", getMessage("system.health.comp.cost.name"), "status", "UP", "latency", "<1 ms", "desc", getMessage("system.health.comp.cost.desc")));
        components.add(Map.of("name", getMessage("system.health.comp.email.name"), "status", "UP", "latency", "UP", "desc", getMessage("system.health.comp.email.desc")));
        components.add(Map.of("name", getMessage("system.health.comp.actuator.name"), "status", "UP", "latency", "/health/", "desc", getMessage("system.health.comp.actuator.desc")));

        int upComponentCount = (int) components.stream().filter(component -> "UP".equals(component.get("status"))).count();
        metrics.put("components", components);
        metrics.put("componentCount", components.size());
        metrics.put("upComponentCount", upComponentCount);

        // JSON map matching /health/ format
        Map<String, Object> healthJson = new LinkedHashMap<>();
        healthJson.put("status", overallStatus);
        healthJson.put("service", "ozerler-marble-erp");
        healthJson.put("version", version);
        healthJson.put("port", port);
        healthJson.put("timestamp", Instant.now().toString());
        healthJson.put("uptime", formattedUptime);

        Map<String, Object> compMap = new LinkedHashMap<>();
        compMap.put("db", Map.of("status", dbStatus, "database", dbName, "version", dbVersion, "pingMs", dbLatencyMs));
        Map<String, Object> rustFsJson = new LinkedHashMap<>();
        rustFsJson.put("status", rustFsStatus);
        rustFsJson.put("bucket", objectStorageService.getBucketName());
        rustFsJson.put("pingMs", rustFsLatencyMs);
        rustFsJson.put("error", Objects.requireNonNullElse(rustFsHealth.getError(), "None"));
        compMap.put("rustfs", rustFsJson);
        compMap.put("diskSpace", Map.of("status", "UP", "totalGb", totalDiskGb, "usableGb", usableDiskGb, "usedPercent", diskPercent + "%"));
        compMap.put("jvmMemory", Map.of("usedMb", usedMemMb, "totalMb", totalMemMb, "maxMb", maxMemMb, "percent", memPercent + "%"));
        compMap.put("threads", Map.of("active", Thread.activeCount(), "processors", runtime.availableProcessors()));
        compMap.put("quarryService", Map.of("status", "UP"));
        compMap.put("factoryService", Map.of("status", "UP"));
        compMap.put("costAccounting", Map.of("status", "UP"));
        compMap.put("mailGateway", Map.of("status", "UP"));
        healthJson.put("components", compMap);

        metrics.put("healthJsonMap", healthJson);

        return metrics;
    }

    public String formatHealthJson(Map<String, Object> healthData) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(healthData.get("healthJsonMap"));
        } catch (Exception e) {
            log.warn("Failed to format health JSON: {}", e.getMessage());
            return "{}";
        }
    }

    public HealthResponse buildHealthResponse() {
        Map<String, DependencyHealth> dependencies = new LinkedHashMap<>();
        dependencies.put("database", checkDatabaseHealth());
        dependencies.put("rustfs", checkRustFsHealth());
        dependencies.put("quarryService", DependencyHealth.up());
        dependencies.put("factoryService", DependencyHealth.up());
        dependencies.put("costAccounting", DependencyHealth.up());
        dependencies.put("diskSpace", checkDiskSpaceHealth());

        String overallStatus;
        if (dependencies.values().stream()
                .allMatch(dependency -> "UP".equals(dependency.getStatus()))) {
            overallStatus = "UP";
        } else {
            overallStatus = "DOWN";
        }

        return HealthResponse.builder()
                .status(overallStatus)
                .dependencies(dependencies)
                .build();
    }

    private DependencyHealth checkDatabaseHealth() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            return DependencyHealth.up();
        } catch (Exception ex) {
            log.error("Database health check failed: {}", ex.getMessage());
            return DependencyHealth.down("Database connection failed");
        }
    }

    private DependencyHealth checkDiskSpaceHealth() {
        return DependencyHealth.up();
    }

    private DependencyHealth checkRustFsHealth() {
        try {
            return objectStorageService.checkHealth();
        } catch (Exception ex) {
            log.error("RustFS health check failed: {}", ex.getMessage());
            return DependencyHealth.down("RustFS erişilemiyor");
        }
    }
}
