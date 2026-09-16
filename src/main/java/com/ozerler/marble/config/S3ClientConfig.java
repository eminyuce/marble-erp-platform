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
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio", matchIfMissing = true)
public class S3ClientConfig {

    private final ObjectStorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        ObjectStorageProperties.Minio minio = requireConfiguredMinio();
        return S3Client.builder()
                .endpointOverride(URI.create(minio.getEndpoint()))
                .region(Region.of(minio.getRegion()))
                .credentialsProvider(staticCredentials(minio))
                .serviceConfiguration(s3Configuration())
                .httpClient(UrlConnectionHttpClient.create())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        ObjectStorageProperties.Minio minio = requireConfiguredMinio();
        String publicEndpoint = minio.getPublicEndpoint();
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            publicEndpoint = minio.getEndpoint();
        }
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.of(minio.getRegion()))
                .credentialsProvider(staticCredentials(minio))
                .serviceConfiguration(s3Configuration())
                .build();
    }

    private static S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }

    private static StaticCredentialsProvider staticCredentials(ObjectStorageProperties.Minio minio) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(minio.getAccessKey(), minio.getSecretKey()));
    }

    private ObjectStorageProperties.Minio requireConfiguredMinio() {
        ObjectStorageProperties.Minio minio = storageProperties.getMinio();
        if (minio.getAccessKey() == null || minio.getAccessKey().isBlank()
                || minio.getSecretKey() == null || minio.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                    "MinIO credentials are not configured. Set MINIO_ACCESS_KEY and MINIO_SECRET_KEY.");
        }
        if (minio.getEndpoint() == null || minio.getEndpoint().isBlank()) {
            throw new IllegalStateException("MinIO endpoint is not configured. Set MINIO_ENDPOINT.");
        }
        if (minio.getBucket() == null || minio.getBucket().isBlank()) {
            throw new IllegalStateException("MinIO bucket is not configured. Set MINIO_BUCKET.");
        }
        return minio;
    }
}
