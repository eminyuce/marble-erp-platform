package com.ozerler.marble.storage;

import com.ozerler.marble.config.ObjectStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;

    private S3ObjectStorageService storage;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getMinio().setBucket("erp-files");
        storage = new S3ObjectStorageService(s3Client, s3Presigner, properties);
    }

    @Test
    @DisplayName("checkHealth returns UP when bucket probe succeeds")
    void checkHealth_WhenMinioReachable_ReturnsUp() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        var health = storage.checkHealth();

        assertThat(health.getStatus()).isEqualTo("UP");
        assertThat(health.getError()).isNull();
    }

    @Test
    @DisplayName("checkHealth returns DOWN when MinIO cannot be reached")
    void checkHealth_WhenMinioUnreachable_ReturnsDown() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var health = storage.checkHealth();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getError()).isEqualTo("MinIO erişilemiyor");
    }

    @Test
    @DisplayName("checkHealth returns DOWN when the write probe fails")
    void checkHealth_WhenWriteProbeFails_ReturnsDown() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("Access Denied"));

        var health = storage.checkHealth();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getError()).isEqualTo("MinIO erişilemiyor");
    }
}
