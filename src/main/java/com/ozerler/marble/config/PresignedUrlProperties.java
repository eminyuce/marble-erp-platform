package com.ozerler.marble.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * How long a browser may use a presigned GET. Images stay readable for a
 * page session. Documents expire sooner because they are more sensitive.
 * Presigned URLs are generated per request and are never stored.
 */
@Configuration
@ConfigurationProperties(prefix = "presigned.url")
@Validated
@Getter
@Setter
public class PresignedUrlProperties {

    public static final int DEFAULT_IMAGE_EXPIRY_MINUTES = 30;

    public static final int DEFAULT_DOCUMENT_EXPIRY_MINUTES = 5;

    @Min(1)
    @Max(1440)
    private int imageExpiryMinutes = DEFAULT_IMAGE_EXPIRY_MINUTES;

    @Min(1)
    @Max(1440)
    private int documentExpiryMinutes = DEFAULT_DOCUMENT_EXPIRY_MINUTES;

    public Duration imageExpiry() {
        return Duration.ofMinutes(imageExpiryMinutes);
    }

    public Duration documentExpiry() {
        return Duration.ofMinutes(documentExpiryMinutes);
    }
}
