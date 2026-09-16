package com.ozerler.marble.storage;

import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory object storage used by the {@code test} profile so Spring Boot tests
 * do not require a live MinIO process.
 */
@Service
@Profile("test")
@ConditionalOnProperty(name = "app.storage.type", havingValue = "memory")
public class InMemoryObjectStorageService implements ObjectStorageService {

    private static final String BUCKET = "erp-files-test";

    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();

    @Override
    public void ensureBucketExists() {
        // No external bucket to create.
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        validateObjectKey(objectKey);
        try {
            byte[] bytes = inputStream.readAllBytes();
            objects.put(objectKey, new StoredObject(bytes, contentType));
        } catch (Exception e) {
            throw new StorageException("Unable to store the uploaded file", e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        validateObjectKey(objectKey);
        StoredObject stored = objects.get(objectKey);
        if (stored == null) {
            throw new StoredFileNotFoundException("Stored object was not found: " + objectKey);
        }
        return new ByteArrayInputStream(stored.content());
    }

    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);
        objects.remove(objectKey);
    }

    @Override
    public boolean exists(String objectKey) {
        validateObjectKey(objectKey);
        return objects.containsKey(objectKey);
    }

    @Override
    public String createPresignedUrl(String objectKey, Duration expiration) {
        validateObjectKey(objectKey);
        if (!exists(objectKey)) {
            throw new StoredFileNotFoundException("Stored object was not found: " + objectKey);
        }
        long seconds = expiration != null ? expiration.toSeconds() : 900;
        return "memory://" + BUCKET + "/" + objectKey + "?expires=" + seconds;
    }

    @Override
    public DependencyHealth checkHealth() {
        return DependencyHealth.up();
    }

    @Override
    public String getBucketName() {
        return BUCKET;
    }

    private static void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.contains("..")) {
            throw new StorageException("Invalid object storage key");
        }
    }

    private record StoredObject(byte[] content, String contentType) {
    }
}
