package com.ozerler.marble.service;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import com.ozerler.marble.storage.FileObjectKeyFactory;
import com.ozerler.marble.storage.ObjectStorageService;
import com.ozerler.marble.util.Filenames;
import com.ozerler.marble.validation.FileUploadValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Idempotent migration of leftover local-disk files into MinIO.
 * Original files are never deleted by this process.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageMigrationService {

    private final FileStorageRepository fileStorageRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageProperties storageProperties;
    private final FileObjectKeyFactory fileObjectKeyFactory;

    @Transactional
    public MigrationResult migrate(boolean dryRun) {
        objectStorageService.ensureBucketExists();
        List<FileStorage> records = fileStorageRepository.findByDeletedFalse();
        int migrated = 0;
        int skipped = 0;
        int failed = 0;

        for (FileStorage record : records) {
            try {
                if (isAlreadyMigrated(record)) {
                    skipped++;
                    continue;
                }
                Path source = resolveSourceFile(record);
                if (source == null) {
                    log.warn("Migration skipped, filesystem file missing fileId={} fileName={}",
                            record.getId(), record.getFileName());
                    skipped++;
                    continue;
                }
                if (dryRun) {
                    log.info("Migration dry-run would upload fileId={} path={} size={}",
                            record.getId(), source, Files.size(source));
                    migrated++;
                    continue;
                }
                migrateRecord(record, source);
                migrated++;
            } catch (Exception e) {
                failed++;
                log.error("Migration failed fileId={} fileName={}", record.getId(), record.getFileName(), e);
            }
        }

        log.info("File storage migration finished dryRun={} migrated={} skipped={} failed={}",
                dryRun, migrated, skipped, failed);
        return new MigrationResult(records.size(), migrated, skipped, failed, dryRun);
    }

    private boolean isAlreadyMigrated(FileStorage record) {
        if (record.getObjectKey() == null || record.getObjectKey().isBlank()) {
            return false;
        }
        return objectStorageService.exists(record.getObjectKey());
    }

    private void migrateRecord(FileStorage record, Path source) throws Exception {
        String extension = Filenames.extension(record.getOriginalName());
        String objectKey = record.getObjectKey();
        if (objectKey == null || objectKey.isBlank()) {
            objectKey = fileObjectKeyFactory.create(record.getEntityType(), record.getEntityId(), extension);
        }

        if (!objectStorageService.exists(objectKey)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size = Files.size(source);
            try (InputStream raw = Files.newInputStream(source);
                 DigestInputStream digestStream = new DigestInputStream(raw, digest)) {
                String mime = record.getMimeType() != null
                        ? record.getMimeType()
                        : FileUploadValidator.detectMimeType(stripDot(extension));
                objectStorageService.upload(objectKey, digestStream, size, mime);
            }
            if (!objectStorageService.exists(objectKey)) {
                throw new IllegalStateException("Upload verification failed for " + objectKey);
            }
            record.setChecksum(HexFormat.of().formatHex(digest.digest()));
        }

        record.setObjectKey(objectKey);
        record.setBucketName(objectStorageService.getBucketName());
        record.setFilePath(com.ozerler.marble.common.Constants.FILE_VIEW_URL_PREFIX + record.getId());
        fileStorageRepository.save(record);
        log.info("Migrated filesystem file to object storage fileId={} objectKey={} source={}",
                record.getId(), objectKey, source);
    }

    private Path resolveSourceFile(FileStorage record) {
        String filename = record.getFileName();
        if (filename == null || filename.isBlank() || Filenames.containsPathTraversal(filename)) {
            return null;
        }
        Path mediaRoot = Paths.get(storageProperties.getMediaDir());
        Path uploadRoot = Paths.get(storageProperties.getUploadDir());
        Path[] candidates = new Path[]{
                mediaRoot.resolve("images").resolve(filename),
                mediaRoot.resolve("documents").resolve(filename),
                mediaRoot.resolve(filename),
                uploadRoot.resolve(filename),
                pathFromStoredFilePath(record.getFilePath())
        };
        for (Path candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Path normalized = candidate.normalize().toAbsolutePath();
            if (Files.isRegularFile(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private Path pathFromStoredFilePath(String filePath) {
        if (filePath == null || filePath.isBlank() || filePath.startsWith("/api/")) {
            return null;
        }
        String relative = filePath;
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return Paths.get(relative);
    }

    private static String stripDot(String extension) {
        if (extension == null || extension.isBlank()) {
            return "";
        }
        String cleaned = extension.startsWith(".") ? extension.substring(1) : extension;
        return cleaned.toLowerCase(Locale.ROOT);
    }

    public record MigrationResult(int examined, int migrated, int skipped, int failed, boolean dryRun) {
    }
}
