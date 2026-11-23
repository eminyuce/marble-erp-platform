package com.ozerler.marble.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class ObjectStorageProperties {

    private static final int DEFAULT_PRESIGN_MINUTES = 15;
    private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    /**
     * {@code rustfs} for production and local Docker; {@code memory} for automated tests.
     */
    private String type = "rustfs";

    /**
     * Local media directory used only to migrate legacy filesystem files.
     */
    private String mediaDir = "media";

    /**
     * Legacy upload directory used only to migrate leftover {@code /uploads} files.
     */
    private String uploadDir = "media";

    private Duration presignedUrlExpiry = Duration.ofMinutes(DEFAULT_PRESIGN_MINUTES);

    private long maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE_BYTES;

    private RustFs rustfs = new RustFs();

    private Migrate migrate = new Migrate();

    @Getter
    @Setter
    public static class RustFs {
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
