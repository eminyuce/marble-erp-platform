package com.ozerler.marble.storage;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.config.PresignedUrlProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

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
        Mockito.when(s3Client.putObject(ArgumentMatchers.any(PutObjectRequest.class), ArgumentMatchers.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        var health = storage.checkHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getError()).isNull();
    }

    @Test
    @DisplayName("checkHealth returns DOWN when RustFS cannot be reached")
    void checkHealth_WhenRustFsUnreachable_ReturnsDown() {
        Mockito.when(s3Client.headBucket(ArgumentMatchers.any(HeadBucketRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var health = storage.checkHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getError()).isEqualTo("RustFS erişilemiyor");
    }

    @Test
    @DisplayName("checkHealth returns DOWN when the write probe fails")
    void checkHealth_WhenWriteProbeFails_ReturnsDown() {
        Mockito.when(s3Client.putObject(ArgumentMatchers.any(PutObjectRequest.class), ArgumentMatchers.any(RequestBody.class)))
                .thenThrow(new RuntimeException("Access Denied"));

        var health = storage.checkHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getError()).isEqualTo("RustFS erişilemiyor");
    }

    @Test
    @DisplayName("presignedImageUrl signs for the configured image expiry and sets the image type")
    void presignedImageUrl_UsesConfiguredImageExpiry() throws MalformedURLException {
        stubPresigner();

        String url = storage.presignedImageUrl("images/block/1/photo.jpg");

        GetObjectPresignRequest request = capturedPresignRequest();
        Assertions.assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(request.getObjectRequest().responseContentType()).isEqualTo("image/jpeg");
        Assertions.assertThat(url).contains("photo.jpg");
    }

    @Test
    @DisplayName("presignedDocumentUrl signs for the configured document expiry")
    void presignedDocumentUrl_UsesConfiguredDocumentExpiry() throws MalformedURLException {
        stubPresigner();

        storage.presignedDocumentUrl("documents/block/1/spec.pdf");

        GetObjectPresignRequest request = capturedPresignRequest();
        Assertions.assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(5));
        Assertions.assertThat(request.getObjectRequest().responseContentType()).isNull();
    }

    @Test
    @DisplayName("image expiry follows presigned.url.image-expiry-minutes without a code change")
    void presignedImageUrl_HonorsOverriddenMinutes() throws MalformedURLException {
        presignedUrlProperties.setImageExpiryMinutes(15);
        stubPresigner();

        storage.presignedImageUrl("images/block/1/photo.png");

        Assertions.assertThat(capturedPresignRequest().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(capturedPresignRequest().getObjectRequest().responseContentType()).isEqualTo("image/png");
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

            Assertions.assertThat(URI.create(imageUrl).getHost()).isEqualTo("files.example.com");
            Assertions.assertThat(URI.create(documentUrl).getHost()).isEqualTo("files.example.com");
            Assertions.assertThat(URI.create(imageUrl).getHost()).isNotEqualTo(URI.create(internalEndpoint).getHost());
            Assertions.assertThat(queryParams(imageUrl)).containsEntry("X-Amz-Expires", "1800");
            Assertions.assertThat(queryParams(documentUrl)).containsEntry("X-Amz-Expires", "300");
        }
    }

    @Test
    @DisplayName("production presigns on the application host under /erp-files/")
    void realPresigner_UsesApplicationHostAndBucketPath() {
        String publicEndpoint = "https://ozerler.naklink.com";
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getRustfs().setBucket("erp-files");
        properties.getRustfs().setEndpoint("http://rustfs:9000");
        properties.getRustfs().setPublicEndpoint(publicEndpoint);
        PresignedUrlProperties expiry = new PresignedUrlProperties();

        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access", "test-secret")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            S3ObjectStorageService realStorage = new S3ObjectStorageService(
                    s3Client, presigner, properties, expiry);
            URI image = URI.create(realStorage.presignedImageUrl("images/block/1/photo.jpg"));

            Assertions.assertThat(image.getScheme()).isEqualTo("https");
            Assertions.assertThat(image.getHost()).isEqualTo("ozerler.naklink.com");
            Assertions.assertThat(image.getPath()).startsWith("/erp-files/");
            Assertions.assertThat(image.getRawQuery()).contains("X-Amz-");
        }
    }

    private static Map<String, String> queryParams(String url) {
        String query = URI.create(url).getRawQuery();
        Map<String, String> params = new HashMap<>();
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

    private void stubPresigner() throws MalformedURLException {
        PresignedGetObjectRequest presigned = Mockito.mock(PresignedGetObjectRequest.class);
        Mockito.when(presigned.url()).thenReturn(URI.create(
                "http://files.example.com/erp-files/images/block/1/photo.jpg?X-Amz-Signature=hidden").toURL());
        Mockito.when(s3Presigner.presignGetObject(ArgumentMatchers.any(GetObjectPresignRequest.class))).thenReturn(presigned);
    }

    private GetObjectPresignRequest capturedPresignRequest() {
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        Mockito.verify(s3Presigner).presignGetObject(captor.capture());
        return captor.getValue();
    }
}
