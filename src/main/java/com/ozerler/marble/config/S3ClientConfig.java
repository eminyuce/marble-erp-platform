package com.ozerler.marble.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "rustfs", matchIfMissing = true)
public class S3ClientConfig {

    private final ObjectStorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        ObjectStorageProperties.RustFs rustFs = requireConfiguredRustFs();
        return S3Client.builder()
                .endpointOverride(URI.create(rustFs.getEndpoint()))
                .region(Region.of(rustFs.getRegion()))
                .credentialsProvider(staticCredentials(rustFs))
                .serviceConfiguration(s3Configuration())
                .httpClient(UrlConnectionHttpClient.create())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        ObjectStorageProperties.RustFs rustFs = requireConfiguredRustFs();
        String publicEndpoint = rustFs.getPublicEndpoint();
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            publicEndpoint = rustFs.getEndpoint();
        }
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.of(rustFs.getRegion()))
                .credentialsProvider(staticCredentials(rustFs))
                .serviceConfiguration(s3Configuration())
                .build();
    }

    private static S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }

    private static StaticCredentialsProvider staticCredentials(ObjectStorageProperties.RustFs rustFs) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(rustFs.getAccessKey(), rustFs.getSecretKey()));
    }

    private ObjectStorageProperties.RustFs requireConfiguredRustFs() {
        ObjectStorageProperties.RustFs rustFs = storageProperties.getRustfs();
        if (rustFs.getAccessKey() == null || rustFs.getAccessKey().isBlank()
                || rustFs.getSecretKey() == null || rustFs.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                    "RustFS credentials are not configured. Set RUSTFS_ACCESS_KEY and RUSTFS_SECRET_KEY.");
        }
        if (rustFs.getEndpoint() == null || rustFs.getEndpoint().isBlank()) {
            throw new IllegalStateException("RustFS endpoint is not configured. Set RUSTFS_ENDPOINT.");
        }
        if (rustFs.getBucket() == null || rustFs.getBucket().isBlank()) {
            throw new IllegalStateException("RustFS bucket is not configured. Set RUSTFS_BUCKET.");
        }
        return rustFs;
    }
}
