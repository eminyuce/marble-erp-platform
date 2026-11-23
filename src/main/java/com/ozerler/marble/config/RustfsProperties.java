package com.ozerler.marble.config;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Canonical RustFS connection. {@code endpoint} is the internal address the
 * application uses (Docker DNS). {@code publicEndpoint} is the host written
 * into presigned URLs so a browser can fetch the object directly.
 */
@Configuration
@ConfigurationProperties(prefix = "rustfs")
@Validated
@Getter
@Setter
public class RustfsProperties {

    @NotBlank
    private String endpoint;

    @NotBlank
    private String publicEndpoint;

    private String accessKey = "";

    private String secretKey = "";

    @NotBlank
    private String bucket = "erp-files";

    @NotBlank
    private String region = "us-east-1";

    /**
     * Startup fails when RustFS is the active store and either credential is blank.
     * The secret is never included in the exception text.
     */
    public void requireCredentials() {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "RustFS credentials are not configured. Set RUSTFS_ACCESS_KEY and RUSTFS_SECRET_KEY.");
        }
    }
}
