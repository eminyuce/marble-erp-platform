package com.ozerler.marble.exception;

/**
 * Raised when object storage (MinIO / S3) cannot complete an operation.
 * Callers must not expose the cause message to end users.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
