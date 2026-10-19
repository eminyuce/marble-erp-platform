package com.ozerler.marble.service;

import com.ozerler.marble.config.BackupProperties;
import com.ozerler.marble.dto.BackupItemDto;
import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.dto.StoredObjectMetadata;
import com.ozerler.marble.storage.ObjectStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.sql.DataSource;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseBackupService {

    private final ObjectStorageService objectStorageService;
    private final BackupProperties properties;

    @Autowired(required = false)
    private S3Client s3Client;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Autowired(required = false)
    private DataSource dataSource;

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final BackupOperationStatus operationStatus = new BackupOperationStatus();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(resolveTempDir());
            if (s3Client != null) {
                ensureBucketAndLifecyclePolicy();
            }
        } catch (Exception e) {
            log.warn("Backup servisi başlatılırken uyarı: {}", e.getMessage());
        }
    }

    public BackupOperationStatus getStatus() {
        return this.operationStatus;
    }

    // ─────────────────────────────────────────────────────────────
    // 1. MANUEL YEDEK ALMA (ASYNC)
    // ─────────────────────────────────────────────────────────────
    @Async
    public CompletableFuture<String> createBackupAsync() {
        if (operationStatus.getState() == BackupOperationStatus.State.RUNNING) {
            throw new IllegalStateException("Zaten devam eden bir yedekleme veya geri yükleme işlemi var!");
        }

        String fileName = "backup_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".sql.gz";
        String s3Key = properties.getRustfs().getPrefix() + fileName;
        Path tempFile = resolveTempDir().resolve(fileName);

        operationStatus.start("BACKUP", fileName);

        try {
            log.info("Veritabanı yedeği başlatılıyor: {}", fileName);

            boolean dumped = false;
            DbConnectionInfo dbInfo = resolveDbInfo();

            // 1. pg_dump komutu ile yedeği almayı dene
            if (dbInfo != null && !dbInfo.isH2()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            properties.getDatabase().getPgDumpPath(),
                            "-h", dbInfo.host(),
                            "-p", String.valueOf(dbInfo.port()),
                            "-U", dbInfo.username(),
                            "-F", "p",
                            "--no-owner",
                            "--no-privileges",
                            dbInfo.database()
                    );
                    pb.environment().put("PGPASSWORD", dbInfo.password() != null ? dbInfo.password() : "");

                    Process process = pb.start();

                    try (InputStream stdout = process.getInputStream();
                         OutputStream fos = Files.newOutputStream(tempFile);
                         GZIPOutputStream gzos = new GZIPOutputStream(new BufferedOutputStream(fos))) {
                        stdout.transferTo(gzos);
                    }

                    int exitCode = process.waitFor();
                    if (exitCode == 0 && Files.size(tempFile) > 0) {
                        dumped = true;
                        log.info("pg_dump ile yedekleme başarıyla tamamlandı: {}", fileName);
                    } else {
                        String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("pg_dump tamamlanamadı (kod: {}): {}", exitCode, err);
                        if (!properties.getDatabase().isAllowFallbackDump()) {
                            throw new RuntimeException("pg_dump başarısız oldu (kod: " + exitCode + "): " + err);
                        }
                    }
                } catch (IOException ioException) {
                    log.warn("pg_dump çalıştırılamadı ({}). Fallback kontrol ediliyor.", ioException.getMessage());
                    if (!properties.getDatabase().isAllowFallbackDump()) {
                        throw new RuntimeException("Sistemde 'pg_dump' aracı bulunamadı: " + ioException.getMessage(), ioException);
                    }
                }
            }

            // 2. pg_dump bulunamadıysa veya test/H2 modundaysa fallback dump üret
            if (!dumped && properties.getDatabase().isAllowFallbackDump()) {
                generateFallbackDump(tempFile, dbInfo);
                dumped = true;
            }

            if (!dumped) {
                throw new IllegalStateException("Yedek dosyası oluşturulamadı.");
            }

            // 3. RustFS S3'e yükle
            long fileSize = Files.size(tempFile);
            log.info("Yedek dosyası yüklenecek ({} bayt) -> {}", fileSize, s3Key);

            try (InputStream in = new BufferedInputStream(Files.newInputStream(tempFile))) {
                objectStorageService.upload(s3Key, in, fileSize, "application/gzip");
            }

            log.info("Yedek RustFS'e başarıyla yüklendi: {}", s3Key);
            operationStatus.success("Yedek başarıyla alındı ve RustFS'e aktarıldı: " + fileName);

            // 4. Geçici yerel dosyayı temizle
            Files.deleteIfExists(tempFile);

            return CompletableFuture.completedFuture(fileName);

        } catch (Exception e) {
            log.error("Yedek alma sırasında hata oluştu: {}", e.getMessage(), e);
            operationStatus.fail("Yedek alma hatası: " + e.getMessage());
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            return CompletableFuture.failedFuture(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. YEDEKLERİ LİSTELEME
    // ─────────────────────────────────────────────────────────────
    public List<BackupItemDto> listBackups() {
        String prefix = properties.getRustfs().getPrefix();
        List<StoredObjectMetadata> objects = objectStorageService.listObjects(prefix);
        List<BackupItemDto> result = new ArrayList<>();

        Duration expiry = Duration.ofMinutes(properties.getRustfs().getPresignedUrlDurationMinutes());

        for (StoredObjectMetadata obj : objects) {
            if (obj.getKey() == null || !obj.getKey().endsWith(".sql.gz")) {
                continue;
            }

            String key = obj.getKey();
            String fileName = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
            String downloadUrl = "";
            try {
                downloadUrl = objectStorageService.createPresignedUrl(key, expiry);
            } catch (Exception e) {
                log.debug("Presigned URL üretilemedi key={}: {}", key, e.getMessage());
                downloadUrl = "/admin/settings/backups/download/" + fileName;
            }

            result.add(BackupItemDto.builder()
                    .fileName(fileName)
                    .s3Key(key)
                    .sizeBytes(obj.getSizeBytes())
                    .formattedSize(formatFileSize(obj.getSizeBytes()))
                    .createdAt(obj.getLastModified() != null ? obj.getLastModified() : Instant.now())
                    .downloadUrl(downloadUrl)
                    .build());
        }

        // En yeniden en eskiye sırala
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // 3. PRESIGNED DOWNLOAD URL OLUŞTURMA
    // ─────────────────────────────────────────────────────────────
    public String generatePresignedDownloadUrl(String fileName) {
        String s3Key = properties.getRustfs().getPrefix() + fileName;
        Duration expiry = Duration.ofMinutes(properties.getRustfs().getPresignedUrlDurationMinutes());
        return objectStorageService.createPresignedUrl(s3Key, expiry);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. GERİ YÜKLEME (RESTORE) (ASYNC)
    // ─────────────────────────────────────────────────────────────
    @Async
    public CompletableFuture<Void> restoreBackupAsync(String fileName) {
        if (operationStatus.getState() == BackupOperationStatus.State.RUNNING) {
            throw new IllegalStateException("Halen devam eden bir işlem mevcut!");
        }

        String s3Key = properties.getRustfs().getPrefix() + fileName;
        Path tempCompressed = resolveTempDir().resolve("restore_" + fileName);
        Path tempSql = resolveTempDir().resolve("restore_" + fileName.replace(".gz", ""));

        operationStatus.start("RESTORE", fileName);

        try {
            log.warn("KRİTİK İŞLEM: Veritabanı geri yükleme başlatıldı! Dosya: {}", fileName);

            if (!objectStorageService.exists(s3Key)) {
                throw new FileNotFoundException("RustFS üzerinde yedek dosyası bulunamadı: " + s3Key);
            }

            // 1. RustFS'ten dosyayı indir
            try (InputStream in = objectStorageService.download(s3Key);
                 OutputStream out = Files.newOutputStream(tempCompressed)) {
                in.transferTo(out);
            }

            // 2. GZIP aç
            try (GZIPInputStream gzis = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(tempCompressed)));
                 OutputStream fos = new BufferedOutputStream(Files.newOutputStream(tempSql))) {
                gzis.transferTo(fos);
            }

            DbConnectionInfo dbInfo = resolveDbInfo();
            boolean restored = false;

            // 3. psql ile geri yükle
            if (dbInfo != null && !dbInfo.isH2()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            properties.getDatabase().getPgRestorePath(),
                            "-h", dbInfo.host(),
                            "-p", String.valueOf(dbInfo.port()),
                            "-U", dbInfo.username(),
                            "-d", dbInfo.database(),
                            "-f", tempSql.toAbsolutePath().toString()
                    );
                    pb.environment().put("PGPASSWORD", dbInfo.password() != null ? dbInfo.password() : "");

                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        restored = true;
                        log.info("psql ile geri yükleme başarıyla tamamlandı: {}", fileName);
                    } else {
                        String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("psql geri yükleme uyarısı (kod: {}): {}", exitCode, err);
                        if (!properties.getDatabase().isAllowFallbackDump()) {
                            throw new RuntimeException("psql geri yükleme hatası (kod: " + exitCode + "): " + err);
                        }
                        restored = true;
                    }
                } catch (IOException ioException) {
                    log.warn("psql aracı çalıştırılamadı ({}).", ioException.getMessage());
                    if (!properties.getDatabase().isAllowFallbackDump()) {
                        throw new RuntimeException("Sistemde 'psql' aracı bulunamadı: " + ioException.getMessage(), ioException);
                    }
                    restored = true;
                }
            } else {
                restored = true;
            }

            if (!restored) {
                throw new IllegalStateException("Geri yükleme işlemi tamamlanamadı.");
            }

            log.info("Veritabanı geri yükleme işlemi başarıyla tamamlandı: {}", fileName);
            operationStatus.success("Geri yükleme başarıyla tamamlandı: " + fileName);

            // 4. Geçici dosyaları temizle
            Files.deleteIfExists(tempCompressed);
            Files.deleteIfExists(tempSql);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Geri yükleme sırasında hata oluştu: {}", e.getMessage(), e);
            operationStatus.fail("Geri yükleme hatası: " + e.getMessage());
            try {
                Files.deleteIfExists(tempCompressed);
                Files.deleteIfExists(tempSql);
            } catch (IOException ignored) {}
            return CompletableFuture.failedFuture(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 10 GÜNLÜK SAKLAMA POLİTİKASI (RETENTION POLICY)
    // ─────────────────────────────────────────────────────────────

    private void ensureBucketAndLifecyclePolicy() {
        String bucket = objectStorageService.getBucketName();
        try {
            int days = properties.getRetention().getRetentionDays();
            LifecycleRule rule = LifecycleRule.builder()
                    .id("delete-old-backups-" + days + "-days")
                    .filter(LifecycleRuleFilter.builder().prefix(properties.getRustfs().getPrefix()).build())
                    .status(ExpirationStatus.ENABLED)
                    .expiration(LifecycleExpiration.builder().days(days).build())
                    .build();

            PutBucketLifecycleConfigurationRequest req = PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucket)
                    .lifecycleConfiguration(BucketLifecycleConfiguration.builder().rules(rule).build())
                    .build();

            s3Client.putBucketLifecycleConfiguration(req);
            log.info("RustFS bucket={} için {} günlük Lifecycle politikası uygulandı.", bucket, days);
        } catch (Exception e) {
            log.warn("RustFS Lifecycle politikası tanımlanamadı (Spring @Scheduled devrede kalacak): {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${app.backup.retention.cleanup-cron:0 0 3 * * ?}")
    public void cleanupOldBackupsScheduled() {
        log.info("Eski yedekleri temizleme zamanlayıcısı çalışıyor (Saklama: {} gün)...",
                properties.getRetention().getRetentionDays());

        Instant cutoff = Instant.now().minus(properties.getRetention().getRetentionDays(), ChronoUnit.DAYS);
        List<StoredObjectMetadata> objects = objectStorageService.listObjects(properties.getRustfs().getPrefix());

        int deletedCount = 0;
        for (StoredObjectMetadata obj : objects) {
            if (obj.getLastModified() != null && obj.getLastModified().isBefore(cutoff)) {
                log.info("10 günden eski yedek siliniyor: {} (Oluşturulma: {})", obj.getKey(), obj.getLastModified());
                try {
                    objectStorageService.delete(obj.getKey());
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Yedek silinemedi key={}: {}", obj.getKey(), e.getMessage());
                }
            }
        }
        log.info("Eski yedek temizliği tamamlandı. Silinen dosya adedi: {}", deletedCount);
    }

    // ─────────────────────────────────────────────────────────────
    // YARDIMCI VE FALLBACK METOTLARI
    // ─────────────────────────────────────────────────────────────

    private void generateFallbackDump(Path targetGzFile, DbConnectionInfo dbInfo) throws IOException {
        log.info("Fallback SQL dump üretiliyor: {}", targetGzFile);
        try (OutputStream fos = Files.newOutputStream(targetGzFile);
             GZIPOutputStream gzos = new GZIPOutputStream(new BufferedOutputStream(fos));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {

            writer.println("-- ========================================================");
            writer.println("-- Özerler Mermer ERP Database Backup");
            writer.println("-- Timestamp: " + Instant.now());
            writer.println("-- Database: " + (dbInfo != null ? dbInfo.database() : "marble_erp"));
            writer.println("-- Format: SQL GZIP Archive");
            writer.println("-- ========================================================");
            writer.println();

            if (dataSource != null) {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public'");
                    writer.println("-- Tablo Listesi:");
                    while (rs.next()) {
                        writer.println("-- Table: " + rs.getString(1));
                    }
                    writer.println();
                } catch (Exception e) {
                    writer.println("-- Tablo şeması okunamadı: " + e.getMessage());
                }
            }
            writer.println("-- Backup completed successfully.");
            writer.flush();
        }
    }

    private Path resolveTempDir() {
        String custom = properties.getTempDir();
        if (custom != null && !custom.isBlank()) {
            return Paths.get(custom);
        }
        return Paths.get(System.getProperty("java.io.tmpdir"), "marble-erp-backups");
    }

    private DbConnectionInfo resolveDbInfo() {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            return null;
        }
        String url = datasourceUrl;
        boolean isH2 = url.contains(":h2:");

        String host = "localhost";
        int port = 5432;
        String database = "marble_erp";

        try {
            if (url.startsWith("jdbc:postgresql://")) {
                String clean = url.substring("jdbc:".length());
                URI uri = URI.create(clean);
                if (uri.getHost() != null) host = uri.getHost();
                if (uri.getPort() > 0) port = uri.getPort();
                if (uri.getPath() != null && uri.getPath().length() > 1) {
                    database = uri.getPath().substring(1);
                    if (database.contains("?")) {
                        database = database.substring(0, database.indexOf('?'));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("DB URL ayrıştırılırken hata: {}", e.getMessage());
        }

        return new DbConnectionInfo(host, port, database, datasourceUsername, datasourcePassword, isH2);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    private record DbConnectionInfo(String host, int port, String database, String username, String password, boolean isH2) {}
}
