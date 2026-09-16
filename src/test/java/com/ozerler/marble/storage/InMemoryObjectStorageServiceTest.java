package com.ozerler.marble.storage;

import com.ozerler.marble.exception.StoredFileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryObjectStorageServiceTest {

    private InMemoryObjectStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryObjectStorageService();
        storage.ensureBucketExists();
    }

    @Test
    @DisplayName("upload, exists, download, presign, and delete work together")
    void lifecycle() throws Exception {
        String key = "images/block/1/a.png";
        byte[] payload = "hello-minio".getBytes(StandardCharsets.UTF_8);

        storage.upload(key, new ByteArrayInputStream(payload), payload.length, "image/png");

        assertThat(storage.exists(key)).isTrue();
        assertThat(storage.download(key).readAllBytes()).isEqualTo(payload);
        assertThat(storage.createPresignedUrl(key, Duration.ofMinutes(5))).contains(key);
        assertThat(storage.checkHealth().getStatus()).isEqualTo("UP");

        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
        assertThatThrownBy(() -> storage.download(key))
                .isInstanceOf(StoredFileNotFoundException.class);
    }
}
