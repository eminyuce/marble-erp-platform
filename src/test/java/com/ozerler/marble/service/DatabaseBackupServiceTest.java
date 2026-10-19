package com.ozerler.marble.service;

import com.ozerler.marble.config.BackupProperties;
import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.dto.BackupItemDto;
import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.dto.StoredObjectMetadata;
import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import com.ozerler.marble.storage.InMemoryObjectStorageService;
import com.ozerler.marble.storage.RustFsBrowserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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

        backupService = new DatabaseBackupService(storageService, properties, internalDockerAccess());
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
    @DisplayName("PostgreSQL backup is not uploaded when it would only list table names")
    void createBackupAsync_doesNotUploadCommentShellForPostgres() throws Exception {
        properties.getDatabase().setPgDumpPath("pg_dump-not-installed");
        setField("datasourceUrl", "jdbc:postgresql://postgres:5432/marble_erp");
        setField("datasourceUsername", "marbleuser");
        setField("datasourcePassword", "secret");

        assertThatThrownBy(() -> backupService.createBackupAsync().get())
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Yedek dosyası oluşturulamadı");
        assertThat(storageService.listObjects("backups/")).isEmpty();
        assertThat(backupService.getStatus().getState()).isEqualTo(BackupOperationStatus.State.FAILED);
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
        assertThat(first.getDownloadUrl())
                .isEqualTo("/admin/settings/backups/download/" + first.getFileName())
                .doesNotContain("rustfs");
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

    @Test
    @DisplayName("openDownload streams the stored gzip without buffering the whole archive first")
    void openDownload_streamsStoredArchive() throws Exception {
        String fileName = backupService.createBackupAsync().get();

        DatabaseBackupService.BackupContent content = backupService.openDownload(fileName);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        backupService.writeBackup(content.body(), output, fileName);

        assertThat(content.sizeBytes()).isEqualTo(output.size());
        try (GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(output.toByteArray()))) {
            String text = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(text).contains("Özerler Mermer ERP Database Backup");
        }
    }

    @Test
    @DisplayName("download rejects a missing backup and an unsafe file name")
    void openDownload_rejectsMissingAndUnsafeNames() {
        assertThatThrownBy(() -> backupService.openDownload("backup_missing.sql.gz"))
                .isInstanceOf(StoredFileNotFoundException.class);

        assertThatThrownBy(() -> backupService.openDownload("../secret.sql.gz"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> backupService.generatePresignedDownloadUrl("backup_missing.sql.gz"))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    @DisplayName("interrupted download is completed without a storage error")
    void writeBackup_treatsBrokenPipeAsInterrupted() throws Exception {
        String fileName = backupService.createBackupAsync().get();
        DatabaseBackupService.BackupContent content = new DatabaseBackupService.BackupContent(
                fileName,
                4,
                "application/gzip",
                brokenPipe());

        backupService.writeBackup(content.body(), new ByteArrayOutputStream(), fileName);
    }

    @Test
    @DisplayName("storage read failure while streaming is reported as a storage error")
    void writeBackup_reportsStorageFailure() throws Exception {
        String fileName = backupService.createBackupAsync().get();

        assertThatThrownBy(() -> backupService.writeBackup(failingStream(), new ByteArrayOutputStream(), fileName))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Yedek indirilemedi");
    }

    @Test
    @DisplayName("public RustFS endpoint produces a presigned URL when the probe succeeds")
    void listBackups_usesPresignedUrlWhenPublicEndpointIsReachable() throws Exception {
        FixedUrlStorage storage = new FixedUrlStorage("https://files.example.com/erp-files/");
        DatabaseBackupService service = serviceWith(storage, "http://rustfs:9000", "https://files.example.com", endpoint -> true);
        String fileName = service.createBackupAsync().get();

        String url = service.listBackups("erp.example.com").get(0).getDownloadUrl();

        assertThat(url).startsWith("https://files.example.com/erp-files/backups/" + fileName);
        assertThat(url).contains("X-Amz-Expires=1800");
        assertThat(url).doesNotContain("rustfs");
        assertThat(service.browserPresignedUrl(fileName, "erp.example.com")).contains(url);
    }

    @Test
    @DisplayName("an internal Docker hostname is never handed to the browser")
    void listBackups_discardsPresignedUrlThatPointsAtDockerDns() throws Exception {
        FixedUrlStorage storage = new FixedUrlStorage("http://rustfs:9000/erp-files/");
        DatabaseBackupService service = serviceWith(storage, "http://rustfs:9000", "https://files.example.com", endpoint -> true);
        String fileName = service.createBackupAsync().get();

        Optional<String> presigned = service.browserPresignedUrl(fileName, "erp.example.com");
        String listed = service.listBackups("erp.example.com").get(0).getDownloadUrl();

        assertThat(presigned).isEmpty();
        assertThat(listed).isEqualTo("/admin/settings/backups/download/" + fileName);
        assertThat(listed).doesNotContain("rustfs");
    }

    @Test
    @DisplayName("unreachable or loopback-only RustFS falls back to the authenticated download path")
    void listBackups_fallsBackWhenBrowserCannotReachRustFs() throws Exception {
        FixedUrlStorage storage = new FixedUrlStorage("http://localhost:9000/erp-files/");
        DatabaseBackupService unreachable = serviceWith(
                storage, "http://rustfs:9000", "http://localhost:9000", endpoint -> false);
        String fileName = unreachable.createBackupAsync().get();

        assertThat(unreachable.listBackups("localhost").get(0).getDownloadUrl())
                .isEqualTo("/admin/settings/backups/download/" + fileName);

        DatabaseBackupService remoteBrowser = serviceWith(
                storage,
                "http://rustfs:9000",
                "http://localhost:9000",
                endpoint -> { throw new AssertionError("loopback must not be probed for a remote browser"); });

        assertThat(remoteBrowser.browserPresignedUrl(fileName, "erp.example.com")).isEmpty();
        assertThat(remoteBrowser.listBackups("erp.example.com").get(0).getDownloadUrl())
                .isEqualTo("/admin/settings/backups/download/" + fileName);
    }

    @Test
    @DisplayName("non-positive presign TTL does not issue a download URL")
    void browserPresignedUrl_skippedWhenTtlIsNotPositive() throws Exception {
        FixedUrlStorage storage = new FixedUrlStorage("https://files.example.com/erp-files/");
        BackupProperties props = copyProperties();
        props.getRustfs().setPresignedUrlDurationMinutes(0);
        DatabaseBackupService service = new DatabaseBackupService(
                storage,
                props,
                browserAccess("http://rustfs:9000", "https://files.example.com", endpoint -> true));
        String fileName = service.createBackupAsync().get();

        assertThat(service.browserPresignedUrl(fileName, "erp.example.com")).isEmpty();
        assertThat(service.listBackups("erp.example.com").get(0).getDownloadUrl())
                .isEqualTo("/admin/settings/backups/download/" + fileName);
    }

    private BackupProperties copyProperties() {
        BackupProperties copy = new BackupProperties();
        copy.setTempDir(tempDir.toString());
        copy.getRustfs().setPrefix("backups/");
        copy.getRustfs().setPresignedUrlDurationMinutes(30);
        copy.getRetention().setRetentionDays(10);
        copy.getDatabase().setAllowFallbackDump(true);
        return copy;
    }

    private DatabaseBackupService serviceWith(
            InMemoryObjectStorageService storage,
            String endpoint,
            String publicEndpoint,
            RustFsBrowserAccess.Probe probe) {
        return new DatabaseBackupService(storage, copyProperties(), browserAccess(endpoint, publicEndpoint, probe));
    }

    private static RustFsBrowserAccess internalDockerAccess() {
        return browserAccess("http://rustfs:9000", "", endpoint -> {
            throw new AssertionError("internal Docker endpoint must not be probed");
        });
    }

    private static RustFsBrowserAccess browserAccess(
            String endpoint, String publicEndpoint, RustFsBrowserAccess.Probe probe) {
        ObjectStorageProperties storageProperties = new ObjectStorageProperties();
        storageProperties.getRustfs().setEndpoint(endpoint);
        storageProperties.getRustfs().setPublicEndpoint(publicEndpoint);
        storageProperties.getRustfs().setBucket("erp-files");
        return new RustFsBrowserAccess(storageProperties, probe);
    }

    private void setField(String name, String value) throws Exception {
        Field field = DatabaseBackupService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(backupService, value);
    }

    private static InputStream brokenPipe() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Broken pipe");
            }
        };
    }

    private static InputStream failingStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("RustFS connection lost");
            }
        };
    }

    private static final class FixedUrlStorage extends InMemoryObjectStorageService {
        private final String urlPrefix;

        private FixedUrlStorage(String urlPrefix) {
            this.urlPrefix = urlPrefix;
        }

        @Override
        public String createPresignedUrl(String objectKey, Duration expiration) {
            long seconds = expiration != null ? expiration.toSeconds() : 0;
            return urlPrefix + objectKey + "?X-Amz-Expires=" + seconds + "&X-Amz-Signature=test";
        }
    }
}
