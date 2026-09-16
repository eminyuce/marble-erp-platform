package com.ozerler.marble.storage;

import com.ozerler.marble.dto.DependencyHealth;

import java.io.InputStream;
import java.time.Duration;

/**
 * S3-compatible object storage port. Business services depend on this
 * abstraction rather than MinIO or AWS SDK types.
 */
public interface ObjectStorageService {

    void ensureBucketExists();

    void upload(String objectKey, InputStream inputStream, long contentLength, String contentType);

    InputStream download(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);

    String createPresignedUrl(String objectKey, Duration expiration);

    DependencyHealth checkHealth();

    String getBucketName();
}
