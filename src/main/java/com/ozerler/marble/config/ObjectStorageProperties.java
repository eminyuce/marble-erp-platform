package com.ozerler.marble.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class ObjectStorageProperties {

    /**
     * {@code minio} for production and local Docker; {@code memory} for automated tests.
     */
    private String type = "minio";

    /**
     * Local media directory used only to migrate legacy filesystem files.
     */
    private String mediaDir = "media";

    /**
     * Legacy upload directory used only to migrate leftover {@code /uploads} files.
     */
    private String uploadDir = "media";

    private Duration presignedUrlExpiry = Duration.ofMinutes(15);

    private long maxFileSizeBytes = 50L * 1024 * 1024;

    private Minio minio = new Minio();

    private Migrate migrate = new Migrate();

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        /**
         * Browser-facing endpoint for presigned URLs. When empty, {@link #endpoint} is used.
         */
        private String publicEndpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private String bucket = "erp-files";
        private String region = "us-east-1";
    }

    @Getter
    @Setter
    public static class Migrate {
        private boolean enabled = false;
        private boolean dryRun = true;
    }
}
