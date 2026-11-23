package com.ozerler.marble.storage;

import java.io.InputStream;
import java.time.Duration;

import com.ozerler.marble.dto.DependencyHealth;

/**
 * S3-compatible object storage port. Business services depend on this
 * abstraction rather than RustFS or AWS SDK types.
 */
public interface ObjectStorageService {

    void ensureBucketExists();

    void upload(String objectKey, InputStream inputStream, long contentLength, String contentType);

    InputStream download(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);

    String createPresignedUrl(String objectKey, Duration expiration);

    /**
     * Browser GET for an image. Expiry comes from {@code presigned.url.image-expiry-minutes}
     * (30 minutes unless overridden). Generated per call and never stored.
     */
    String presignedImageUrl(String objectKey);

    /**
     * Browser GET for a document. Expiry comes from {@code presigned.url.document-expiry-minutes}
     * (5 minutes unless overridden). Generated per call and never stored.
     */
    String presignedDocumentUrl(String objectKey);

    DependencyHealth checkHealth();

    String getBucketName();

    java.util.List<com.ozerler.marble.dto.StoredObjectMetadata> listObjects(String prefix);
}
