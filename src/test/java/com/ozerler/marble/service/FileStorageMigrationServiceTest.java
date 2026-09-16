package com.ozerler.marble.service;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import com.ozerler.marble.storage.FileObjectKeyFactory;
import com.ozerler.marble.storage.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageMigrationServiceTest {

    @Mock
    private FileStorageRepository fileStorageRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @TempDir
    Path tempDir;

    private ObjectStorageProperties properties;
    private FileStorageMigrationService migrationService;

    @BeforeEach
    void setUp() {
        properties = new ObjectStorageProperties();
        properties.setMediaDir(tempDir.toString());
        properties.setUploadDir(tempDir.toString());
        migrationService = new FileStorageMigrationService(
                fileStorageRepository, objectStorageService, properties, new FileObjectKeyFactory());
    }

    @Test
    @DisplayName("dry-run reports files that would be uploaded without writing to MinIO")
    void dryRunDoesNotUpload() throws Exception {
        Path images = Files.createDirectories(tempDir.resolve("images"));
        Files.writeString(images.resolve("legacy.jpg"), "jpeg-bytes");
        FileStorage record = FileStorage.builder()
                .id(7L)
                .fileName("legacy.jpg")
                .originalName("block.jpg")
                .mimeType("image/jpeg")
                .fileSize(10L)
                .filePath("/media/images/legacy.jpg")
                .entityType("BLOCK")
                .entityId(3L)
                .build();
        when(fileStorageRepository.findByDeletedFalse()).thenReturn(List.of(record));

        FileStorageMigrationService.MigrationResult result = migrationService.migrate(true);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.migrated()).isEqualTo(1);
        verify(objectStorageService, never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(fileStorageRepository, never()).save(any());
    }

    @Test
    @DisplayName("already migrated objects are skipped when rerun")
    void alreadyMigratedIsSkipped() {
        FileStorage record = FileStorage.builder()
                .id(8L)
                .fileName("done.jpg")
                .objectKey("images/block/1/done.jpg")
                .originalName("done.jpg")
                .mimeType("image/jpeg")
                .fileSize(4L)
                .filePath("/api/upload/view/8")
                .entityType("BLOCK")
                .entityId(1L)
                .build();
        when(fileStorageRepository.findByDeletedFalse()).thenReturn(List.of(record));
        when(objectStorageService.exists("images/block/1/done.jpg")).thenReturn(true);

        FileStorageMigrationService.MigrationResult result = migrationService.migrate(false);

        assertThat(result.skipped()).isEqualTo(1);
        verify(objectStorageService, never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    @DisplayName("legacy filesystem file is uploaded and metadata is updated")
    void migratesLegacyFile() throws Exception {
        Path images = Files.createDirectories(tempDir.resolve("images"));
        Files.writeString(images.resolve("legacy.jpg"), "jpeg-bytes");
        FileStorage record = FileStorage.builder()
                .id(9L)
                .fileName("legacy.jpg")
                .originalName("block.jpg")
                .mimeType("image/jpeg")
                .fileSize(10L)
                .filePath("/media/images/legacy.jpg")
                .entityType("BLOCK")
                .entityId(3L)
                .build();
        when(fileStorageRepository.findByDeletedFalse()).thenReturn(List.of(record));
        when(objectStorageService.getBucketName()).thenReturn("erp-files");
        when(objectStorageService.exists(anyString())).thenReturn(false, true);
        when(fileStorageRepository.save(any(FileStorage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileStorageMigrationService.MigrationResult result = migrationService.migrate(false);

        assertThat(result.migrated()).isEqualTo(1);
        assertThat(record.getObjectKey()).startsWith("images/block/3/");
        assertThat(record.getFilePath()).isEqualTo("/api/upload/view/9");
        assertThat(record.getBucketName()).isEqualTo("erp-files");
        assertThat(Files.exists(images.resolve("legacy.jpg"))).isTrue();
        verify(objectStorageService).upload(anyString(), any(InputStream.class), anyLong(), anyString());
    }
}
