package com.ozerler.marble.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.backup")
@Getter
@Setter
public class BackupProperties {

    private boolean enabled = true;
    private String tempDir = "";
    private RustFsProperties rustfs = new RustFsProperties();
    private DatabaseProperties database = new DatabaseProperties();
    private RetentionProperties retention = new RetentionProperties();

    @Getter
    @Setter
    public static class RustFsProperties {
        private String prefix = "backups/";
        private int presignedUrlDurationMinutes = 30;
    }

    @Getter
    @Setter
    public static class DatabaseProperties {
        private String pgDumpPath = "pg_dump";
        private String pgRestorePath = "psql";
        private boolean allowFallbackDump = true;
    }

    @Getter
    @Setter
    public static class RetentionProperties {
        private int retentionDays = 10;
        private String cleanupCron = "0 0 3 * * ?";
    }
}
