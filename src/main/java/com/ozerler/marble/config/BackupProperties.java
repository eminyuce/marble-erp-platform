package com.ozerler.marble.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.backup")
@Getter
@Setter
public class BackupProperties {

    private static final int DEFAULT_PRESIGN_MINUTES = 30;

    private boolean enabled = true;
    private String tempDir = "";
    private RustFsProperties rustfs = new RustFsProperties();
    private DatabaseProperties database = new DatabaseProperties();
    private RetentionProperties retention = new RetentionProperties();

    @Getter
    @Setter
    public static class RustFsProperties {
        private String prefix = "backups/";
        private int presignedUrlDurationMinutes = DEFAULT_PRESIGN_MINUTES;
    }

    @Getter
    @Setter
    public static class DatabaseProperties {
        private String pgDumpPath = "pg_dump";
        private String pgRestorePath = "psql";
        private String dockerContainer = "";
        private boolean allowFallbackDump = true;
    }

    @Getter
    @Setter
    public static class RetentionProperties {
        private int retentionDays = 10;
        private String cleanupCron = "0 0 3 * * ?";
    }
}
