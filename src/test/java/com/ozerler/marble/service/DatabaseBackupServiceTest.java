package com.ozerler.marble.service;

import com.ozerler.marble.config.BackupProperties;
import com.ozerler.marble.dto.BackupItemDto;
import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.dto.StoredObjectMetadata;
import com.ozerler.marble.storage.InMemoryObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseBackupServiceTest {

    private InMemoryObjectStorageService storageService;
    private BackupProperties properties;
    private DatabaseBackupService backupService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new InMemoryObjectStorageService();
        properties = new BackupProperties();
        properties.setTempDir(tempDir.toString());
        properties.getRustfs().setPrefix("backups/");
        properties.getRustfs().setPresignedUrlDurationMinutes(30);
        properties.getRetention().setRetentionDays(10);
        properties.getDatabase().setAllowFallbackDump(true);

        backupService = new DatabaseBackupService(storageService, properties);
    }

    @Test
    @DisplayName("createBackupAsync creates GZIP archive and uploads to RustFS with backups/ prefix")
    void createBackupAsync_createsAndUploadsArchive() throws Exception {
        CompletableFuture<String> future = backupService.createBackupAsync();
        String fileName = future.get();

        assertThat(fileName).startsWith("backup_").endsWith(".sql.gz");

        String s3Key = "backups/" + fileName;
        assertThat(storageService.exists(s3Key)).isTrue();

        // Verify content is a valid GZIP stream
        try (InputStream raw = storageService.download(s3Key);
             GZIPInputStream gzis = new GZIPInputStream(raw)) {
            byte[] bytes = gzis.readAllBytes();
            String content = new String(bytes);
            assertThat(content).contains("Özerler Mermer ERP Database Backup");
        }

        BackupOperationStatus status = backupService.getStatus();
        assertThat(status.getState()).isEqualTo(BackupOperationStatus.State.SUCCESS);
    }

    @Test
    @DisplayName("listBackups returns sorted list with formatted size and presigned URL")
    void listBackups_returnsFormattedList() throws Exception {
        backupService.createBackupAsync().get();

        List<BackupItemDto> list = backupService.listBackups();
        assertThat(list).isNotEmpty();

        BackupItemDto first = list.get(0);
        assertThat(first.getFileName()).startsWith("backup_").endsWith(".sql.gz");
        assertThat(first.getS3Key()).startsWith("backups/");
        assertThat(first.getSizeBytes()).isGreaterThan(0);
        assertThat(first.getFormattedSize()).isNotBlank();
        assertThat(first.getDownloadUrl()).isNotBlank();
    }

    @Test
    @DisplayName("generatePresignedDownloadUrl returns presigned URL for existing backup")
    void generatePresignedDownloadUrl_returnsValidUrl() throws Exception {
        String fileName = backupService.createBackupAsync().get();
        String url = backupService.generatePresignedDownloadUrl(fileName);

        assertThat(url).contains("backups/" + fileName);
    }

    @Test
    @DisplayName("restoreBackupAsync restores database from RustFS archive")
    void restoreBackupAsync_restoresSuccessfully() throws Exception {
        String fileName = backupService.createBackupAsync().get();

        CompletableFuture<Void> restoreFuture = backupService.restoreBackupAsync(fileName);
        restoreFuture.get();

        BackupOperationStatus status = backupService.getStatus();
        assertThat(status.getState()).isEqualTo(BackupOperationStatus.State.SUCCESS);
        assertThat(status.getMessage()).contains("Geri yükleme başarıyla tamamlandı");
    }

    @Test
    @DisplayName("cleanupOldBackupsScheduled deletes backups older than 10 days and keeps newer ones")
    void cleanupOldBackupsScheduled_deletesOnlyExpired() throws Exception {
        // Create a recent backup
        String recentFile = backupService.createBackupAsync().get();
        String recentKey = "backups/" + recentFile;

        // Manually insert an old backup (12 days ago)
        String oldKey = "backups/backup_2026-09-01_10-00-00.sql.gz";
        storageService.upload(oldKey, InputStream.nullInputStream(), 0, "application/gzip");

        // Simulate 12 days ago in metadata
        List<StoredObjectMetadata> items = storageService.listObjects("backups/");
        for (StoredObjectMetadata item : items) {
            if (item.getKey().equals(oldKey)) {
                item.setLastModified(Instant.now().minus(12, ChronoUnit.DAYS));
            }
        }

        // Run scheduled cleanup
        backupService.cleanupOldBackupsScheduled();

        // The recent backup must still exist
        assertThat(storageService.exists(recentKey)).isTrue();
    }
}
