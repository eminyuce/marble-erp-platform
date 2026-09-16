package com.ozerler.marble.storage;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio", matchIfMissing = true)
public class S3ObjectStorageService implements ObjectStorageService {

    private static final String HEALTH_PROBE_PREFIX = ".health-probe/";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ObjectStorageProperties storageProperties;

    @PostConstruct
    public void initialize() {
        ensureBucketExists();
    }

    @Override
    public void ensureBucketExists() {
        String bucket = getBucketName();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Object storage bucket is available bucket={}", bucket);
        } catch (NoSuchBucketException notFound) {
            createPrivateBucket(bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                createPrivateBucket(bucket);
                return;
            }
            throw new StorageException("Unable to verify object storage bucket", e);
        }
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        validateObjectKey(objectKey);
        String bucket = getBucketName();
        try {
            PutObjectRequest.Builder request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentLength(contentLength);
            if (contentType != null && !contentType.isBlank()) {
                request.contentType(contentType);
            }
            s3Client.putObject(request.build(), RequestBody.fromInputStream(inputStream, contentLength));
            log.info("Object upload completed objectKey={} contentLength={}", objectKey, contentLength);
        } catch (S3Exception e) {
            log.error("Object upload failed objectKey={}", objectKey, e);
            throw new StorageException("Unable to store the uploaded file", e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        validateObjectKey(objectKey);
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(objectKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new StoredFileNotFoundException("Stored object was not found: " + objectKey);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new StoredFileNotFoundException("Stored object was not found: " + objectKey);
            }
            log.error("Object download failed objectKey={}", objectKey, e);
            throw new StorageException("Unable to download the requested file", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(objectKey)
                    .build());
            log.info("Object delete completed objectKey={}", objectKey);
        } catch (S3Exception e) {
            log.error("Object delete failed objectKey={}", objectKey, e);
            throw new StorageException("Unable to delete the stored file", e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        validateObjectKey(objectKey);
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(objectKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new StorageException("Unable to check whether the stored file exists", e);
        }
    }

    @Override
    public String createPresignedUrl(String objectKey, Duration expiration) {
        validateObjectKey(objectKey);
        Duration ttl = expiration != null ? expiration : storageProperties.getPresignedUrlExpiry();
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(getBucketName())
                            .key(objectKey)
                            .build())
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (S3Exception e) {
            log.error("Presigned URL generation failed objectKey={}", objectKey, e);
            throw new StorageException("Unable to create a download link", e);
        }
    }

    @Override
    public DependencyHealth checkHealth() {
        String bucket = getBucketName();
        String probeKey = HEALTH_PROBE_PREFIX + UUID.randomUUID();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            byte[] probe = "health-check-ok".getBytes();
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(probeKey).contentLength((long) probe.length).build(),
                    RequestBody.fromBytes(probe));
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(probeKey).build());
            return DependencyHealth.up();
        } catch (Exception e) {
            log.error("Object storage health check failed bucket={}", bucket, e);
            return DependencyHealth.down("MinIO erişilemiyor");
        }
    }

    @Override
    public String getBucketName() {
        return storageProperties.getMinio().getBucket();
    }

    private void createPrivateBucket(String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created private object storage bucket={}", bucket);
        } catch (S3Exception e) {
            throw new StorageException("Unable to create object storage bucket", e);
        }
    }

    private static void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.contains("..")) {
            throw new StorageException("Invalid object storage key");
        }
    }
}
