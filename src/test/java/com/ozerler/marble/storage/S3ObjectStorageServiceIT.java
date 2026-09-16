package com.ozerler.marble.storage;

import com.ozerler.marble.config.ObjectStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class S3ObjectStorageServiceIT {

    private static final String ACCESS_KEY = "testkey";
    private static final String SECRET_KEY = "testsecret";
    private static final String BUCKET = "erp-files";

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z"))
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).forStatusCode(200));

    @Test
    @DisplayName("S3-compatible MinIO client can upload, download, exist-check, presign, and delete")
    void objectLifecycleAgainstMinio() throws Exception {
        String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getMinio().setEndpoint(endpoint);
        properties.getMinio().setPublicEndpoint(endpoint);
        properties.getMinio().setAccessKey(ACCESS_KEY);
        properties.getMinio().setSecretKey(SECRET_KEY);
        properties.getMinio().setBucket(BUCKET);
        properties.getMinio().setRegion("us-east-1");

        try (S3Client s3Client = s3Client(endpoint);
             S3Presigner presigner = s3Presigner(endpoint)) {
            S3ObjectStorageService storage = new S3ObjectStorageService(s3Client, presigner, properties);
            storage.ensureBucketExists();

            String key = "documents/block/1/sample.txt";
            byte[] payload = "minio-integration".getBytes(StandardCharsets.UTF_8);
            storage.upload(key, new ByteArrayInputStream(payload), payload.length, "text/plain");

            assertThat(storage.exists(key)).isTrue();
            assertThat(storage.download(key).readAllBytes()).isEqualTo(payload);
            assertThat(storage.createPresignedUrl(key, Duration.ofMinutes(5)))
                    .startsWith(endpoint)
                    .contains(key);
            assertThat(storage.checkHealth().getStatus()).isEqualTo("UP");

            storage.delete(key);
            assertThat(storage.exists(key)).isFalse();
        }
    }

    private static S3Client s3Client(String endpoint) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClient(UrlConnectionHttpClient.create())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    private static S3Presigner s3Presigner(String endpoint) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
