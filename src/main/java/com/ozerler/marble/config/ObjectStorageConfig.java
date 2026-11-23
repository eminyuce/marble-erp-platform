package com.ozerler.marble.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Two endpoints on purpose. {@link S3Client} talks to {@code rustfs.endpoint},
 * which is the internal address ({@code http://rustfs:9000} inside Compose).
 * {@link S3Presigner} stamps {@code rustfs.public-endpoint} into the signature
 * so the browser fetches the object from RustFS directly and never needs Docker
 * DNS. Upload, delete, and head stay on the internal client. Both use path-style
 * access, the same region, and the same credentials. The bucket stays private.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "rustfs", matchIfMissing = true)
public class ObjectStorageConfig {

    private final RustfsProperties rustfsProperties;
    private final ObjectStorageProperties storageProperties;
    private final Environment environment;

    @Bean
    public S3Client s3Client() {
        RustfsProperties rustfs = requireConfigured();
        return S3Client.builder()
                .endpointOverride(URI.create(rustfs.getEndpoint().trim()))
                .region(Region.of(rustfs.getRegion()))
                .credentialsProvider(staticCredentials(rustfs))
                .serviceConfiguration(s3Configuration())
                .httpClient(UrlConnectionHttpClient.create())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        RustfsProperties rustfs = requireConfigured();
        return S3Presigner.builder()
                .endpointOverride(URI.create(rustfs.getPublicEndpoint().trim()))
                .region(Region.of(rustfs.getRegion()))
                .credentialsProvider(staticCredentials(rustfs))
                .serviceConfiguration(s3Configuration())
                .build();
    }

    private RustfsProperties requireConfigured() {
        rustfsProperties.requireCredentials();
        if (rustfsProperties.getEndpoint() == null || rustfsProperties.getEndpoint().isBlank()) {
            throw new IllegalStateException("RustFS endpoint is not configured. Set RUSTFS_ENDPOINT.");
        }
        if (rustfsProperties.getPublicEndpoint() == null || rustfsProperties.getPublicEndpoint().isBlank()) {
            throw new IllegalStateException(
                    "RustFS public endpoint is not configured. Set RUSTFS_PUBLIC_ENDPOINT.");
        }
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            RustfsPublicEndpointPolicy.requireHttpsPublicHost(rustfsProperties.getPublicEndpoint());
        }
        alignLegacyStorageProperties();
        return rustfsProperties;
    }

    /**
     * Existing browser-access and bucket readers still use {@code app.storage.rustfs}.
     * Copy the canonical {@code rustfs.*} values so those readers cannot drift.
     */
    private void alignLegacyStorageProperties() {
        ObjectStorageProperties.RustFs target = storageProperties.getRustfs();
        target.setEndpoint(rustfsProperties.getEndpoint());
        target.setPublicEndpoint(rustfsProperties.getPublicEndpoint());
        target.setAccessKey(rustfsProperties.getAccessKey());
        target.setSecretKey(rustfsProperties.getSecretKey());
        target.setBucket(rustfsProperties.getBucket());
        target.setRegion(rustfsProperties.getRegion());
    }

    private static S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }

    private static StaticCredentialsProvider staticCredentials(RustfsProperties rustfs) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(rustfs.getAccessKey(), rustfs.getSecretKey()));
    }
}
