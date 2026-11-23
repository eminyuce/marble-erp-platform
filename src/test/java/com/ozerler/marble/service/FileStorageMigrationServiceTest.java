package com.ozerler.marble.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import com.ozerler.marble.storage.FileObjectKeyFactory;
import com.ozerler.marble.storage.ObjectStorageService;

@ExtendWith(MockitoExtension.class)
class FileStorageMigrationServiceTest {

    @Mock
    private FileStorageRepository fileStorageRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @TempDir
    private Path tempDir;

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
    @DisplayName("dry-run reports files that would be uploaded without writing to RustFS")
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
        Mockito.when(fileStorageRepository.findByDeletedFalse()).thenReturn(List.of(record));

        FileStorageMigrationService.MigrationResult result = migrationService.migrate(true);

        Assertions.assertThat(result.dryRun()).isTrue();
        Assertions.assertThat(result.migrated()).isEqualTo(1);
        Mockito.verify(objectStorageService, Mockito.never()).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any(InputStream.class), ArgumentMatchers.anyLong(), ArgumentMatchers.anyString());
        Mockito.verify(fileStorageRepository, Mockito.never()).save(ArgumentMatchers.any());
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
        Mockito.when(fileStorageRepository.findByDeletedFalse()).thenReturn(List.of(record));
        Mockito.when(objectStorageService.exists("images/block/1/done.jpg")).thenReturn(true);

        FileStorageMigrationService.MigrationResult result = migrationService.migrate(false);

        Assertions.assertThat(result.skipped()).isEqualTo(1);
        Mockito.verify(objectStorageService, Mockito.never()).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any(InputStream.class), ArgumentMatchers.anyLong(), ArgumentMatchers.anyString());
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
        Mockito.when(fileStorageRepository.findByDeletedFalse()).thenReturn(List.of(record));
        Mockito.when(objectStorageService.getBucketName()).thenReturn("erp-files");
        Mockito.when(objectStorageService.exists(ArgumentMatchers.anyString())).thenReturn(false, true);
        Mockito.when(fileStorageRepository.save(ArgumentMatchers.any(FileStorage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileStorageMigrationService.MigrationResult result = migrationService.migrate(false);

        Assertions.assertThat(result.migrated()).isEqualTo(1);
        Assertions.assertThat(record.getObjectKey()).startsWith("images/block/3/");
        Assertions.assertThat(record.getFilePath()).isEqualTo("/api/upload/view/9");
        Assertions.assertThat(record.getBucketName()).isEqualTo("erp-files");
        Assertions.assertThat(Files.exists(images.resolve("legacy.jpg"))).isTrue();
        Mockito.verify(objectStorageService).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any(InputStream.class), ArgumentMatchers.anyLong(), ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("missing stored MIME type is detected from the file bytes")
    void detectsMimeWhenRecordHasNone() throws Exception {
        Path documents = Files.createDirectories(tempDir.resolve("documents"));
        Files.writeString(documents.resolve("notes.txt"), "ocak notu");
        FileStorage record = FileStorage.builder()
                .id(12L)
                .fileName("notes.txt")
                .originalName("notes.txt")
                .fileSize(9L)
                .filePath("/media/documents/notes.txt")
                .entityType("BLOCK")
                .entityId(4L)
                .build();
        Mockito.when(fileStorageRepository.findByDeletedFalse()).thenReturn(List.of(record));
        Mockito.when(objectStorageService.getBucketName()).thenReturn("erp-files");
        Mockito.when(objectStorageService.exists(ArgumentMatchers.anyString())).thenReturn(false, true);
        Mockito.when(fileStorageRepository.save(ArgumentMatchers.any(FileStorage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileStorageMigrationService.MigrationResult result = migrationService.migrate(false);

        Assertions.assertThat(result.migrated()).isEqualTo(1);
        Mockito.verify(objectStorageService).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any(InputStream.class), ArgumentMatchers.anyLong(), ArgumentMatchers.eq("text/plain"));
    }
}
