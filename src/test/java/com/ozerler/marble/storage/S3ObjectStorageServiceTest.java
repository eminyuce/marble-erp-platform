package com.ozerler.marble.storage;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.slf4j.LoggerFactory;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.config.PresignedUrlProperties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
    void checkHealthWhenRustFsReachableReturnsUp() {
        Mockito.when(
                        s3Client.putObject(
                                ArgumentMatchers.any(PutObjectRequest.class),
                                ArgumentMatchers.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        var health = storage.checkHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getError()).isNull();
    }

    @Test
    @DisplayName("checkHealth returns DOWN when RustFS cannot be reached")
    void checkHealthWhenRustFsUnreachableReturnsDown() {
        Mockito.when(s3Client.headBucket(ArgumentMatchers.any(HeadBucketRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var health = storage.checkHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getError()).isEqualTo("RustFS erişilemiyor");
    }

    @Test
    @DisplayName("checkHealth returns DOWN when the write probe fails")
    void checkHealthWhenWriteProbeFailsReturnsDown() {
        Mockito.when(
                        s3Client.putObject(
                                ArgumentMatchers.any(PutObjectRequest.class),
                                ArgumentMatchers.any(RequestBody.class)))
                .thenThrow(new RuntimeException("Access Denied"));

        var health = storage.checkHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getError()).isEqualTo("RustFS erişilemiyor");
    }

    @Test
    @DisplayName("presignedImageUrl signs for the configured image expiry and sets the image type")
    void presignedImageUrlUsesConfiguredImageExpiry() throws MalformedURLException {
        stubPresigner();

        String url = storage.presignedImageUrl("images/block/1/photo.jpg");

        GetObjectPresignRequest request = capturedPresignRequest();
        Assertions.assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(request.getObjectRequest().responseContentType()).isEqualTo("image/jpeg");
        Assertions.assertThat(url).contains("photo.jpg");
    }

    @Test
    @DisplayName("presignedDocumentUrl signs for the configured document expiry")
    void presignedDocumentUrlUsesConfiguredDocumentExpiry() throws MalformedURLException {
        stubPresigner();

        storage.presignedDocumentUrl("documents/block/1/spec.pdf");

        GetObjectPresignRequest request = capturedPresignRequest();
        Assertions.assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(5));
        Assertions.assertThat(request.getObjectRequest().responseContentType()).isNull();
    }

    @Test
    @DisplayName("image expiry follows presigned.url.image-expiry-minutes without a code change")
    void presignedImageUrlHonorsOverriddenMinutes() throws MalformedURLException {
        presignedUrlProperties.setImageExpiryMinutes(15);
        stubPresigner();

        storage.presignedImageUrl("images/block/1/photo.png");

        Assertions.assertThat(capturedPresignRequest().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        Assertions.assertThat(capturedPresignRequest().getObjectRequest().responseContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("a real presigner stamps the public host and the image or document expiry")
    void realPresignerUsesPublicHostAndConfiguredExpiry() {
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
    void realPresignerUsesApplicationHostAndBucketPath() {
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
        Mockito.when(
                        s3Presigner.presignGetObject(
                                ArgumentMatchers.any(GetObjectPresignRequest.class)))
                .thenReturn(presigned);
    }

    private GetObjectPresignRequest capturedPresignRequest() {
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        Mockito.verify(s3Presigner).presignGetObject(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("checkHealth logs exception class and message once, then throttles repeats")
    void checkHealthLogsCauseOnceAndThrottlesRepeats() {
        Mockito.when(s3Client.headBucket(ArgumentMatchers.any(HeadBucketRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        MutableClock healthClock = new MutableClock();
        storage.setClock(healthClock);
        storage.setHealthFailureLogInterval(Duration.ofMinutes(5));

        LoggerProbe probe = LoggerProbe.attach();
        try {
            Assertions.assertThat(storage.checkHealth().getStatus()).isEqualTo("DOWN");
            Assertions.assertThat(storage.checkHealth().getStatus()).isEqualTo("DOWN");

            List<String> errors = probe.messages(Level.ERROR);
            Assertions.assertThat(errors).hasSize(1);
            Assertions.assertThat(errors.get(0))
                    .contains("bucket=erp-files")
                    .contains("java.lang.RuntimeException")
                    .contains("Connection refused");

            healthClock.advance(Duration.ofMinutes(5));
            Assertions.assertThat(storage.checkHealth().getStatus()).isEqualTo("DOWN");
            Assertions.assertThat(probe.messages(Level.ERROR)).hasSize(2);
        } finally {
            probe.detach();
        }
    }

    @Test
    @DisplayName("checkHealth recovery resets throttling so the next outage is logged again")
    void checkHealthRecoveryAllowsAFreshFailureLog() {
        LoggerProbe probe = LoggerProbe.attach();
        try {
            Mockito.when(s3Client.headBucket(ArgumentMatchers.any(HeadBucketRequest.class)))
                    .thenThrow(new RuntimeException("Connection refused"));
            storage.checkHealth();

            Mockito.reset(s3Client);
            Mockito.when(
                            s3Client.putObject(
                                    ArgumentMatchers.any(PutObjectRequest.class),
                                    ArgumentMatchers.any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            Assertions.assertThat(storage.checkHealth().getStatus()).isEqualTo("UP");
            Assertions.assertThat(probe.messages(Level.INFO))
                    .anyMatch(message -> message.contains("recovered") && message.contains("erp-files"));

            Mockito.when(s3Client.headBucket(ArgumentMatchers.any(HeadBucketRequest.class)))
                    .thenThrow(new RuntimeException("Connection refused"));
            storage.checkHealth();
            Assertions.assertThat(probe.messages(Level.ERROR)).hasSize(2);
        } finally {
            probe.detach();
        }
    }

    @Test
    @DisplayName("health failure messages redact credential-like values")
    void sanitizeHealthMessageRedactsSecrets() {
        String raw = "Unable to execute token=super-secret&X-Amz-Signature=abcd1234 AWSAccessKeyId=AKIAEXAMPLE";
        String sanitized = S3ObjectStorageService.sanitizeHealthMessage(raw);
        Assertions.assertThat(sanitized)
                .doesNotContain("super-secret")
                .doesNotContain("abcd1234")
                .doesNotContain("AKIAEXAMPLE")
                .contains("***");
    }

    private static final class LoggerProbe {
        private final Logger logger;
        private final Level previousLevel;
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        private LoggerProbe(Logger logger) {
            this.logger = logger;
            this.previousLevel = logger.getLevel();
        }

        static LoggerProbe attach() {
            Logger logger = (Logger) LoggerFactory.getLogger(S3ObjectStorageService.class);
            LoggerProbe probe = new LoggerProbe(logger);
            probe.appender.start();
            logger.addAppender(probe.appender);
            logger.setLevel(Level.DEBUG);
            return probe;
        }

        void detach() {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        List<String> messages(Level level) {
            List<String> result = new ArrayList<>();
            for (ILoggingEvent event : appender.list) {
                if (event.getLevel().equals(level)) {
                    result.add(event.getFormattedMessage());
                }
            }
            return result;
        }
    }

    private static final class MutableClock extends java.time.Clock {
        private Instant instant = Instant.parse("2026-11-17T00:22:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(ZoneId zone) {
            return java.time.Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
