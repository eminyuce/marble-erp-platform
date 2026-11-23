package com.ozerler.marble.storage;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.config.PresignedUrlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;

    private PresignedUrlProperties presignedUrlProperties;
    private S3ObjectStorageService storage;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getRustfs().setBucket("erp-files");
        presignedUrlProperties = new PresignedUrlProperties();
        storage = new S3ObjectStorageService(s3Client, s3Presigner, properties, presignedUrlProperties);
    }

    @Test
    @DisplayName("checkHealth returns UP when bucket probe succeeds")
    void checkHealth_WhenRustFsReachable_ReturnsUp() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        var health = storage.checkHealth();

        assertThat(health.getStatus()).isEqualTo("UP");
        assertThat(health.getError()).isNull();
    }

    @Test
    @DisplayName("checkHealth returns DOWN when RustFS cannot be reached")
    void checkHealth_WhenRustFsUnreachable_ReturnsDown() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var health = storage.checkHealth();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getError()).isEqualTo("RustFS erişilemiyor");
    }

    @Test
    @DisplayName("checkHealth returns DOWN when the write probe fails")
    void checkHealth_WhenWriteProbeFails_ReturnsDown() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("Access Denied"));

        var health = storage.checkHealth();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getError()).isEqualTo("RustFS erişilemiyor");
    }

    @Test
    @DisplayName("presignedImageUrl signs for the configured image expiry and sets the image type")
    void presignedImageUrl_UsesConfiguredImageExpiry() throws MalformedURLException {
        stubPresigner();

        String url = storage.presignedImageUrl("images/block/1/photo.jpg");

        GetObjectPresignRequest request = capturedPresignRequest();
        assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(request.getObjectRequest().responseContentType()).isEqualTo("image/jpeg");
        assertThat(url).contains("photo.jpg");
    }

    @Test
    @DisplayName("presignedDocumentUrl signs for the configured document expiry")
    void presignedDocumentUrl_UsesConfiguredDocumentExpiry() throws MalformedURLException {
        stubPresigner();

        storage.presignedDocumentUrl("documents/block/1/spec.pdf");

        GetObjectPresignRequest request = capturedPresignRequest();
        assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(request.getObjectRequest().responseContentType()).isNull();
    }

    @Test
    @DisplayName("image expiry follows presigned.url.image-expiry-minutes without a code change")
    void presignedImageUrl_HonorsOverriddenMinutes() throws MalformedURLException {
        presignedUrlProperties.setImageExpiryMinutes(15);
        stubPresigner();

        storage.presignedImageUrl("images/block/1/photo.png");

        assertThat(capturedPresignRequest().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(capturedPresignRequest().getObjectRequest().responseContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("a real presigner stamps the public host and the image or document expiry")
    void realPresigner_UsesPublicHostAndConfiguredExpiry() {
        String internalEndpoint = "http://rustfs:9000";
        String publicEndpoint = "http://files.example.com";
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getRustfs().setBucket("erp-files");
        properties.getRustfs().setEndpoint(internalEndpoint);
        properties.getRustfs().setPublicEndpoint(publicEndpoint);
        PresignedUrlProperties expiry = new PresignedUrlProperties();
        expiry.setImageExpiryMinutes(30);
        expiry.setDocumentExpiryMinutes(5);

        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access", "test-secret")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            S3ObjectStorageService realStorage = new S3ObjectStorageService(
                    s3Client, presigner, properties, expiry);
            String imageUrl = realStorage.presignedImageUrl("images/block/1/photo.jpg");
            String documentUrl = realStorage.presignedDocumentUrl("documents/block/1/spec.pdf");

            assertThat(URI.create(imageUrl).getHost()).isEqualTo("files.example.com");
            assertThat(URI.create(documentUrl).getHost()).isEqualTo("files.example.com");
            assertThat(URI.create(imageUrl).getHost()).isNotEqualTo(URI.create(internalEndpoint).getHost());
            assertThat(queryParams(imageUrl)).containsEntry("X-Amz-Expires", "1800");
            assertThat(queryParams(documentUrl)).containsEntry("X-Amz-Expires", "300");
        }
    }

    private static Map<String, String> queryParams(String url) {
        String query = URI.create(url).getRawQuery();
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            params.put(key, value);
        }
        return params;
    }

    private void stubPresigner() throws MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create(
                "http://files.example.com/erp-files/images/block/1/photo.jpg?X-Amz-Signature=hidden").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
    }

    private GetObjectPresignRequest capturedPresignRequest() {
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        org.mockito.Mockito.verify(s3Presigner).presignGetObject(captor.capture());
        return captor.getValue();
    }
}
