package com.ozerler.marble.storage;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.config.PresignedUrlProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers(disabledWithoutDocker = true)
class S3ObjectStorageServiceContainerTest {

    private static final String ACCESS_KEY = "rustfstest";
    private static final String SECRET_KEY = "rustfstestsecret";
    private static final String BUCKET = "erp-files";

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> RUSTFS = new GenericContainer<>(DockerImageName.parse("rustfs/rustfs:1.0.1"))
            .withEnv("RUSTFS_ACCESS_KEY", ACCESS_KEY)
            .withEnv("RUSTFS_SECRET_KEY", SECRET_KEY)
            .withEnv("RUSTFS_ADDRESS", ":9000")
            .withEnv("RUSTFS_CONSOLE_ENABLE", "false")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/health").forPort(9000).forStatusCode(200));

    @Test
    @DisplayName("S3-compatible RustFS client can upload, download, exist-check, presign, and delete")
    void objectLifecycleAgainstRustFs() throws Exception {
        String endpoint = "http://" + RUSTFS.getHost() + ":" + RUSTFS.getMappedPort(9000);
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getRustfs().setEndpoint(endpoint);
        properties.getRustfs().setPublicEndpoint(endpoint);
        properties.getRustfs().setAccessKey(ACCESS_KEY);
        properties.getRustfs().setSecretKey(SECRET_KEY);
        properties.getRustfs().setBucket(BUCKET);
        properties.getRustfs().setRegion("us-east-1");

        try (S3Client s3Client = s3Client(endpoint);
                S3Presigner presigner = s3Presigner(endpoint)) {
            S3ObjectStorageService storage = new S3ObjectStorageService(
                    s3Client, presigner, properties, new PresignedUrlProperties());
            storage.ensureBucketExists();

            String key = "documents/block/1/sample.txt";
            byte[] payload = "rustfs-integration".getBytes(StandardCharsets.UTF_8);
            storage.upload(key, new ByteArrayInputStream(payload), payload.length, "text/plain");

            Assertions.assertThat(storage.exists(key)).isTrue();
            Assertions.assertThat(storage.download(key).readAllBytes()).isEqualTo(payload);
            Assertions.assertThat(storage.createPresignedUrl(key, Duration.ofMinutes(5)))
                    .startsWith(endpoint)
                    .contains(key);
            Assertions.assertThat(storage.checkHealth().getStatus()).isEqualTo("UP");

            storage.delete(key);
            Assertions.assertThat(storage.exists(key)).isFalse();
        }
    }

    @Test
    @DisplayName("presigned image and document URLs use the public host and configured expiry")
    void presignedUrlsUsePublicEndpointAndDistinctExpiries() {
        String internalEndpoint = "http://" + RUSTFS.getHost() + ":" + RUSTFS.getMappedPort(9000);
        String publicEndpoint = "http://files.example.com";
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getRustfs().setEndpoint(internalEndpoint);
        properties.getRustfs().setPublicEndpoint(publicEndpoint);
        properties.getRustfs().setBucket(BUCKET);
        properties.getRustfs().setRegion("us-east-1");

        PresignedUrlProperties expiry = new PresignedUrlProperties();
        expiry.setImageExpiryMinutes(30);
        expiry.setDocumentExpiryMinutes(5);

        try (S3Client s3Client = s3Client(internalEndpoint);
                S3Presigner presigner = s3Presigner(publicEndpoint)) {
            S3ObjectStorageService storage = new S3ObjectStorageService(s3Client, presigner, properties, expiry);

            String imageUrl = storage.presignedImageUrl("images/block/1/photo.jpg");
            String documentUrl = storage.presignedDocumentUrl("documents/block/1/spec.pdf");

            URI image = URI.create(imageUrl);
            URI document = URI.create(documentUrl);
            Assertions.assertThat(image.getHost()).isEqualTo(URI.create(publicEndpoint).getHost());
            Assertions.assertThat(document.getHost()).isEqualTo(URI.create(publicEndpoint).getHost());
            Assertions.assertThat(image.getHost()).isNotEqualTo(URI.create(internalEndpoint).getHost());
            Assertions.assertThat(image.getAuthority()).doesNotContain(":" + RUSTFS.getMappedPort(9000));
            Assertions.assertThat(queryParams(imageUrl)).containsEntry("X-Amz-Expires", "1800");
            Assertions.assertThat(queryParams(documentUrl)).containsEntry("X-Amz-Expires", "300");
            Assertions.assertThat(imageUrl).contains("response-content-type=");
        }
    }

    private static Map<String, String> queryParams(String url) {
        String query = URI.create(url).getRawQuery();
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            String key;
            if (separator < 0) {
                key = pair;
            } else {
                key = pair.substring(0, separator);
            }
            String value;
            if (separator < 0) {
                value = "";
            } else {
                value = pair.substring(separator + 1);
            }
            params.put(key, value);
        }
        return params;
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
