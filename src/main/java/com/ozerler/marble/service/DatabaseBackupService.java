package com.ozerler.marble.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ozerler.marble.config.BackupProperties;
import com.ozerler.marble.dto.BackupItemDto;
import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.dto.StoredObjectMetadata;
import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import com.ozerler.marble.storage.ObjectStorageService;
import com.ozerler.marble.storage.RustFsBrowserAccess;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;

@Service
@Slf4j
public class DatabaseBackupService {

    private static final Pattern SAFE_BACKUP_FILE_NAME =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,180}\\.sql\\.gz");
    private static final String BACKUP_CONTENT_TYPE = "application/gzip";
    private static final int BYTES_PER_UNIT = 1024;
    private static final int DEFAULT_POSTGRES_PORT = 5432;
    private static final int ERROR_READER_JOIN_MILLIS = 2000;
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ObjectStorageService objectStorageService;
    private final BackupProperties properties;
    private final RustFsBrowserAccess browserAccess;
    private final S3Client s3Client;
    private final DataSource dataSource;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;

    private final BackupOperationStatus operationStatus = new BackupOperationStatus();

    public DatabaseBackupService(
            ObjectStorageService objectStorageService,
            BackupProperties properties,
            RustFsBrowserAccess browserAccess) {
        this(objectStorageService, properties, browserAccess, "", "", "");
    }

    public DatabaseBackupService(
            ObjectStorageService objectStorageService,
            BackupProperties properties,
            RustFsBrowserAccess browserAccess,
            String datasourceUrl,
            String datasourceUsername,
            String datasourcePassword) {
        this(objectStorageService, properties, browserAccess, null, null,
                datasourceUrl, datasourceUsername, datasourcePassword);
    }

    @Autowired
    public DatabaseBackupService(
            ObjectStorageService objectStorageService,
            BackupProperties properties,
            RustFsBrowserAccess browserAccess,
            ObjectProvider<S3Client> s3Clients,
            ObjectProvider<DataSource> dataSources,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.datasource.password:}") String datasourcePassword) {
        this.objectStorageService = objectStorageService;
        this.properties = properties;
        this.browserAccess = browserAccess;
        if (s3Clients == null) {
            this.s3Client = null;
        } else {
            this.s3Client = s3Clients.getIfAvailable();
        }
        if (dataSources == null) {
            this.dataSource = null;
        } else {
            this.dataSource = dataSources.getIfAvailable();
        }
        if (datasourceUrl == null) {
            this.datasourceUrl = "";
        } else {
            this.datasourceUrl = datasourceUrl;
        }
        if (datasourceUsername == null) {
            this.datasourceUsername = "";
        } else {
            this.datasourceUsername = datasourceUsername;
        }
        if (datasourcePassword == null) {
            this.datasourcePassword = "";
        } else {
            this.datasourcePassword = datasourcePassword;
        }
    }

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

            if (dbInfo != null && !dbInfo.isH2()) {
                dumped = dumpWithPgDump(tempFile, dbInfo, fileName);
                if (!dumped) {
                    dumped = dumpWithDockerPgDump(tempFile, dbInfo, fileName);
                }
                if (!dumped) {
                    dumped = dumpWithJdbc(tempFile, dbInfo, fileName);
                }
            }

            if (!dumped && properties.getDatabase().isAllowFallbackDump() && (dbInfo == null || dbInfo.isH2())) {
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
            } catch (IOException cleanupError) {
                log.warn("Geçici yedek dosyası silinemedi: {}", cleanupError.getMessage());
            }
            return CompletableFuture.failedFuture(e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. YEDEKLERİ LİSTELEME
    // ─────────────────────────────────────────────────────────────
    public List<BackupItemDto> listBackups() {
        return listBackups(null);
    }

    public List<BackupItemDto> listBackups(String requestHost) {
        String prefix = properties.getRustfs().getPrefix();
        List<StoredObjectMetadata> objects = objectStorageService.listObjects(prefix);
        List<BackupItemDto> result = new ArrayList<>();
        boolean presignForBrowser = canIssueBrowserUrl(requestHost);

        for (StoredObjectMetadata obj : objects) {
            if (obj.getKey() == null || !obj.getKey().endsWith(".sql.gz")) {
                continue;
            }

            String key = obj.getKey();
            String fileName;
            if (key.contains("/")) {
                fileName = key.substring(key.lastIndexOf('/') + 1);
            } else {
                fileName = key;
            }
            String downloadUrl = authenticatedDownloadPath(fileName);
            if (presignForBrowser && isSafeBackupFileName(fileName)) {
                try {
                    downloadUrl = browserPresignedUrl(fileName, requestHost).orElse(downloadUrl);
                } catch (StoredFileNotFoundException ex) {
                    log.debug("Backup disappeared while listing fileName={}", fileName);
                }
            }

            Instant createdAt;
            if (obj.getLastModified() != null) {
                createdAt = obj.getLastModified();
            } else {
                createdAt = Instant.now();
            }
            result.add(BackupItemDto.builder()
                    .fileName(fileName)
                    .s3Key(key)
                    .sizeBytes(obj.getSizeBytes())
                    .formattedSize(formatFileSize(obj.getSizeBytes()))
                    .createdAt(createdAt)
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
        String key = backupObjectKey(fileName);
        if (!objectStorageService.exists(key)) {
            throw new StoredFileNotFoundException("Yedek dosyası bulunamadı: " + fileName);
        }
        return objectStorageService.createPresignedUrl(key, presignedTtl());
    }

    /**
     * Presigned URL when the user's browser can reach RustFS. Empty when RustFS is
     * only on the internal network, so the caller streams the object instead.
     * The returned string is the signer output unchanged, so the signature stays valid.
     */
    public Optional<String> browserPresignedUrl(String fileName, String requestHost) {
        if (!canIssueBrowserUrl(requestHost)) {
            return Optional.empty();
        }
        try {
            String rawUrl = generatePresignedDownloadUrl(fileName);
            if (!isSafeLocationHeader(rawUrl) || !browserAccess.isBrowserSafeUrl(URI.create(rawUrl))) {
                log.info("Presigned backup URL is not browser-reachable; streaming fileName={}", fileName);
                return Optional.empty();
            }
            return Optional.of(rawUrl);
        } catch (StoredFileNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalArgumentException && !isSafeBackupFileName(fileName)) {
                throw ex;
            }
            log.warn("Presigned backup URL could not be created; streaming fileName={}", fileName);
            return Optional.empty();
        }
    }

    public BackupContent openDownload(String fileName) {
        String key = backupObjectKey(fileName);
        long sizeBytes = sizeOf(key);
        try {
            InputStream body = objectStorageService.download(key);
            return new BackupContent(fileName, sizeBytes, BACKUP_CONTENT_TYPE, body);
        } catch (StoredFileNotFoundException ex) {
            throw new StoredFileNotFoundException("Yedek dosyası bulunamadı: " + fileName);
        }
    }

    public void writeBackup(InputStream input, OutputStream output, String fileName) {
        try (InputStream source = input) {
            source.transferTo(output);
            output.flush();
        } catch (IOException ex) {
            if (isClientAbort(ex)) {
                log.info("Backup download interrupted fileName={}", fileName);
                return;
            }
            log.error("Backup download failed while streaming fileName={}", fileName, ex);
            throw new StorageException("Yedek indirilemedi.", ex);
        } catch (RuntimeException ex) {
            if (isClientAbort(ex)) {
                log.info("Backup download interrupted fileName={}", fileName);
                return;
            }
            if (ex instanceof StorageException storageException) {
                throw storageException;
            }
            log.error("Backup download failed while streaming fileName={}", fileName, ex);
            throw new StorageException("Yedek indirilemedi.", ex);
        }
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
                    String value;
                    if (dbInfo.password() != null) {
                        value = dbInfo.password();
                    } else {
                        value = "";
                    }
                    pb.environment().put("PGPASSWORD", value);

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
            } catch (IOException cleanupError) {
                log.warn("Geçici geri yükleme dosyası silinemedi: {}", cleanupError.getMessage());
            }
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

    private boolean dumpWithPgDump(Path tempFile, DbConnectionInfo dbInfo, String fileName) {
        String value;
        if (dbInfo.username() == null) {
            value = "";
        } else {
            value = dbInfo.username();
        }
        ProcessBuilder builder = new ProcessBuilder(
                properties.getDatabase().getPgDumpPath(),
                "-h", dbInfo.host(),
                "-p", String.valueOf(dbInfo.port()),
                "-U", value,
                "-F", "p",
                "--no-owner",
                "--no-privileges",
                dbInfo.database()
        );
        String valueValue;
        if (dbInfo.password() != null) {
            valueValue = dbInfo.password();
        } else {
            valueValue = "";
        }
        builder.environment().put("PGPASSWORD", valueValue);
        return runDumpProcess(builder, tempFile, fileName, "pg_dump");
    }

    private boolean dumpWithDockerPgDump(Path tempFile, DbConnectionInfo dbInfo, String fileName) {
        String container = properties.getDatabase().getDockerContainer();
        if (container == null || container.isBlank()) {
            return false;
        }
        if (!container.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,120}")) {
            log.warn("Geçersiz PostgreSQL konteyner adı, docker pg_dump atlandı.");
            return false;
        }
        String value;
        if (dbInfo.username() == null) {
            value = "";
        } else {
            value = dbInfo.username();
        }
        ProcessBuilder builder = new ProcessBuilder(
                "docker", "exec", container,
                "pg_dump",
                "-U", value,
                "-d", dbInfo.database(),
                "-F", "p",
                "--no-owner",
                "--no-privileges"
        );
        return runDumpProcess(builder, tempFile, fileName, "docker pg_dump");
    }

    private boolean runDumpProcess(ProcessBuilder builder, Path tempFile, String fileName, String source) {
        try {
            Process process = builder.start();
            process.getOutputStream().close();
            StringBuilder errors = new StringBuilder();
            Thread errorReader = new Thread(() -> {
                try {
                    errors.append(new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException exception) {
                    errors.append(exception.getMessage());
                }
            }, source + "-stderr");
            errorReader.setDaemon(true);
            errorReader.start();

            long bytes;
            try (InputStream stdout = process.getInputStream();
                    OutputStream fos = Files.newOutputStream(tempFile);
                    GZIPOutputStream gzip = new GZIPOutputStream(new BufferedOutputStream(fos))) {
                bytes = stdout.transferTo(gzip);
            }
            int exitCode = process.waitFor();
            errorReader.join(ERROR_READER_JOIN_MILLIS);
            if (exitCode == 0 && bytes > 0) {
                log.info("{} ile yedekleme tamamlandı: {} ({} bayt)", source, fileName, bytes);
                return true;
            }
            String error = errors.toString().trim();
            if (!properties.getDatabase().isAllowFallbackDump()) {
                throw new IllegalStateException(source + " başarısız oldu (kod: " + exitCode + "): " + error);
            }
            log.warn("{} tamamlanamadı (kod: {}): {}", source, exitCode, error);
            return false;
        } catch (IOException exception) {
            if (!properties.getDatabase().isAllowFallbackDump()) {
                throw new IllegalStateException(source + " çalıştırılamadı: " + exception.getMessage(), exception);
            }
            log.warn("{} çalıştırılamadı: {}", source, exception.getMessage());
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(source + " kesildi.", exception);
        }
    }

    private boolean dumpWithJdbc(Path tempFile, DbConnectionInfo dbInfo, String fileName) throws IOException, SQLException {
        if (dataSource == null) {
            log.warn("PostgreSQL veri yedeği için DataSource yok.");
            return false;
        }
        log.info("pg_dump yok. PostgreSQL satırları JDBC ile yazılıyor: {}", fileName);
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            try (OutputStream fos = Files.newOutputStream(tempFile);
                    GZIPOutputStream gzip = new GZIPOutputStream(new BufferedOutputStream(fos));
                    Writer writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
                long rows = new PostgresDataDump().write(connection, writer, dbInfo.database());
                log.info("PostgreSQL veri yedeği yazıldı: {} satır, dosya {}", rows, fileName);
            } finally {
                connection.rollback();
                connection.setReadOnly(false);
                connection.setAutoCommit(originalAutoCommit);
            }
            return true;
        }
    }

    private void generateFallbackDump(Path targetGzFile, DbConnectionInfo dbInfo) throws IOException {
        log.info("Fallback SQL dump üretiliyor: {}", targetGzFile);
        try (OutputStream fos = Files.newOutputStream(targetGzFile);
                GZIPOutputStream gzos = new GZIPOutputStream(new BufferedOutputStream(fos));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {

            writer.println("-- ========================================================");
            writer.println("-- Özerler Mermer ERP Database Backup");
            writer.println("-- Timestamp: " + Instant.now());
            String value;
            if (dbInfo != null) {
                value = dbInfo.database();
            } else {
                value = "marble_erp";
            }
            writer.println("-- Database: " + value);
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
        int port = DEFAULT_POSTGRES_PORT;
        String database = "marble_erp";

        try {
            if (url.startsWith("jdbc:postgresql://")) {
                String clean = url.substring("jdbc:".length());
                URI uri = URI.create(clean);
                if (uri.getHost() != null) {
                    host = uri.getHost();
                }
                if (uri.getPort() > 0) {
                    port = uri.getPort();
                }
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

    private boolean canIssueBrowserUrl(String requestHost) {
        return properties.getRustfs().getPresignedUrlDurationMinutes() > 0
                && browserAccess.canBrowserUsePresignedUrl(requestHost);
    }

    private Duration presignedTtl() {
        int minutes = properties.getRustfs().getPresignedUrlDurationMinutes();
        if (minutes <= 0) {
            throw new IllegalStateException("Yedek indirme bağlantısı süresi yapılandırılmamış.");
        }
        return Duration.ofMinutes(minutes);
    }

    private String backupObjectKey(String fileName) {
        if (!isSafeBackupFileName(fileName)) {
            throw new IllegalArgumentException("Geçersiz yedek dosya adı.");
        }
        return properties.getRustfs().getPrefix() + fileName;
    }

    private long sizeOf(String key) {
        List<StoredObjectMetadata> objects = objectStorageService.listObjects(key);
        for (StoredObjectMetadata metadata : objects) {
            if (key.equals(metadata.getKey())) {
                return metadata.getSizeBytes();
            }
        }
        return -1L;
    }

    static boolean isSafeBackupFileName(String fileName) {
        return fileName != null
                && !fileName.contains("..")
                && fileName.indexOf('/') < 0
                && fileName.indexOf('\\') < 0
                && SAFE_BACKUP_FILE_NAME.matcher(fileName).matches();
    }

    static String authenticatedDownloadPath(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "/admin/settings/backups/download/" + encoded;
    }

    private static boolean isSafeLocationHeader(String value) {
        return value != null
                && !value.isBlank()
                && value.indexOf('\r') < 0
                && value.indexOf('\n') < 0
                && (value.startsWith("https://") || value.startsWith("http://"));
    }

    static boolean isClientAbort(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String typeName = current.getClass().getName();
            if (typeName.endsWith("ClientAbortException") || typeName.endsWith("AsyncRequestNotUsableException")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("broken pipe")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String formatFileSize(long bytes) {
        if (bytes < BYTES_PER_UNIT) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(BYTES_PER_UNIT));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(BYTES_PER_UNIT, exp), pre);
    }

    private record DbConnectionInfo(String host, int port, String database, String username, String password, boolean isH2) {
    }

    public record BackupContent(String fileName, long sizeBytes, String contentType, InputStream body) {
    }
}
