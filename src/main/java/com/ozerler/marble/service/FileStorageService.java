package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.exception.FileAccessDeniedException;
import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import com.ozerler.marble.security.SecurityUtils;
import com.ozerler.marble.storage.FileObjectKeyFactory;
import com.ozerler.marble.storage.ObjectStorageService;
import com.ozerler.marble.util.Filenames;
import com.ozerler.marble.validation.FileUploadValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageRepository fileStorageRepository;
    private final org.springframework.context.MessageSource messageSource;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageProperties storageProperties;
    private final FileUploadValidator fileUploadValidator;
    private final FileObjectKeyFactory fileObjectKeyFactory;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    /**
     * Backward-compatible single file storage without explicit entity binding.
     */
    @Transactional
    public String storeFile(MultipartFile file) throws IOException {
        FileStorage saved = storeFile(file, "GENERAL", null);
        return saved.getFilePath();
    }

    /**
     * Validates the upload, streams the binary to object storage, then persists metadata.
     */
    @Transactional
    public FileStorage storeFile(MultipartFile file, String entityType, Long entityId) throws IOException {
        FileUploadValidator.ValidatedUpload validated = fileUploadValidator.validate(file);
        String entity = (entityType != null && !entityType.isBlank()) ? entityType.trim().toUpperCase() : "BLOCK";
        String objectKey = fileObjectKeyFactory.create(entity, entityId, validated.cleanExtension());

        log.info("File upload started entityType={} entityId={} objectKey={}", entity, entityId, objectKey);

        String checksum;
        try {
            checksum = uploadToObjectStorage(file, validated, objectKey);
        } catch (RuntimeException e) {
            log.error("File upload failed entityType={} entityId={} objectKey={}", entity, entityId, objectKey, e);
            throw e;
        }

        String uniqueFilename = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        FileStorage fileStorage = FileStorage.builder()
                .fileName(uniqueFilename)
                .originalName(validated.originalFilename())
                .mimeType(validated.mimeType())
                .fileSize(validated.fileSize())
                .filePath("")
                .objectKey(objectKey)
                .bucketName(objectStorageService.getBucketName())
                .checksum(checksum)
                .entityType(entity)
                .entityId(entityId)
                .deleted(false)
                .build();

        try {
            FileStorage saved = fileStorageRepository.save(fileStorage);
            saved.setFilePath(Constants.FILE_VIEW_URL_PREFIX + saved.getId());
            FileStorage persisted = fileStorageRepository.save(saved);
            log.info("File upload completed fileId={} entityType={} entityId={} objectKey={}",
                    persisted.getId(), entity, entityId, objectKey);
            return persisted;
        } catch (RuntimeException e) {
            log.error("File upload metadata save failed entityType={} entityId={} objectKey={}",
                    entity, entityId, objectKey, e);
            deleteStoredObjectQuietly(objectKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<FileStorage> getFilesForEntity(String entityType, Long entityId) {
        if (entityType == null || entityId == null) {
            return Collections.emptyList();
        }
        return fileStorageRepository.findByEntityTypeAndEntityIdAndDeletedFalseOrderByCreatedDateAsc(
                entityType.trim().toUpperCase(), entityId);
    }

    @Transactional(readOnly = true)
    public Optional<FileStorage> getFileById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return fileStorageRepository.findByIdAndDeletedFalse(id)
                .filter(this::canCurrentUserAccess);
    }

    @Transactional
    public void attachFilesToEntity(List<Long> fileIds, String entityType, Long entityId) {
        if (fileIds == null || fileIds.isEmpty() || entityId == null) {
            return;
        }
        String entity = entityType != null ? entityType.trim().toUpperCase() : "BLOCK";
        List<FileStorage> files = fileStorageRepository.findByIdInAndDeletedFalse(fileIds);
        for (FileStorage file : files) {
            assertCanModify(file);
            file.setEntityType(entity);
            file.setEntityId(entityId);
        }
        fileStorageRepository.saveAll(files);
    }

    /**
     * Soft-deletes metadata first, then removes the object. A storage failure leaves an
     * inaccessible orphan that can be retried later without restoring the file to users.
     */
    @Transactional
    public boolean deleteFileRecord(Long fileId) {
        if (fileId == null) {
            return false;
        }

        Optional<FileStorage> fileOpt = fileStorageRepository.findByIdAndDeletedFalse(fileId);
        if (fileOpt.isEmpty()) {
            return false;
        }

        FileStorage fileStorage = fileOpt.get();
        assertCanModify(fileStorage);
        softDelete(fileStorage);
        deleteBinary(fileStorage);
        log.info("File delete completed fileId={} entityType={} entityId={} objectKey={}",
                fileId, fileStorage.getEntityType(), fileStorage.getEntityId(), fileStorage.getObjectKey());
        return true;
    }

    @Transactional
    public void deleteAllFilesForEntity(String entityType, Long entityId) {
        if (entityType == null || entityId == null) {
            return;
        }
        List<FileStorage> files = fileStorageRepository.findByEntityTypeAndEntityId(
                entityType.trim().toUpperCase(), entityId);
        for (FileStorage file : files) {
            if (file.isDeleted()) {
                continue;
            }
            softDelete(file);
            deleteBinary(file);
        }
        if (!files.isEmpty()) {
            log.info("Deleted {} files for entityType={} entityId={}", files.size(), entityType, entityId);
        }
    }

    @Transactional
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null) {
            return false;
        }
        String payload = fileUrl.trim();
        Long idFromUrl = extractIdFromApplicationUrl(payload);
        if (idFromUrl != null) {
            return deleteFileRecord(idFromUrl);
        }

        String filename = extractFilenameFromUrl(payload);
        if (filename == null || filename.isBlank() || Filenames.containsPathTraversal(filename)) {
            log.warn("Invalid file deletion path rejected");
            return false;
        }

        Optional<FileStorage> matching = fileStorageRepository.findFirstByFileNameAndDeletedFalse(filename);
        if (matching.isPresent()) {
            return deleteFileRecord(matching.get().getId());
        }
        return deleteLegacyPhysicalFile(filename);
    }

    public Resource loadAsResource(Long fileId) {
        FileStorage fileStorage = requireAccessibleFile(fileId);
        if (fileStorage.getObjectKey() != null && !fileStorage.getObjectKey().isBlank()) {
            InputStream stream = objectStorageService.download(fileStorage.getObjectKey());
            return new ObjectStorageResource(stream, fileStorage);
        }
        return loadLegacyFilesystemResource(fileStorage);
    }

    public String createPresignedDownloadUrl(Long fileId) {
        FileStorage fileStorage = requireAccessibleFile(fileId);
        if (fileStorage.getObjectKey() == null || fileStorage.getObjectKey().isBlank()) {
            throw new StorageException("File has not been migrated to object storage yet");
        }
        Duration expiry = storageProperties.getPresignedUrlExpiry();
        log.info("Presigned download URL created fileId={} entityType={} entityId={} objectKey={} expirySeconds={}",
                fileId, fileStorage.getEntityType(), fileStorage.getEntityId(), fileStorage.getObjectKey(),
                expiry.toSeconds());
        return objectStorageService.createPresignedUrl(fileStorage.getObjectKey(), expiry);
    }

    public Duration getPresignedUrlExpiry() {
        return storageProperties.getPresignedUrlExpiry();
    }

    public FileStorage requireAccessibleFile(Long fileId) {
        FileStorage fileStorage = fileStorageRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new StoredFileNotFoundException(getMessage("error.file.not_found", fileId)));
        assertCanAccess(fileStorage);
        return fileStorage;
    }

    public DependencyHealth checkStorageHealth() {
        return objectStorageService.checkHealth();
    }

    public String getImagesDirName() {
        return objectStorageService.getBucketName();
    }

    public String getDocumentsDirName() {
        return objectStorageService.getBucketName();
    }

    private String uploadToObjectStorage(
            MultipartFile file, FileUploadValidator.ValidatedUpload validated, String objectKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (validated.svgBytes() != null) {
                try (DigestInputStream digestStream =
                             new DigestInputStream(new ByteArrayInputStream(validated.svgBytes()), digest)) {
                    objectStorageService.upload(
                            objectKey, digestStream, validated.svgBytes().length, validated.mimeType());
                }
            } else {
                try (InputStream raw = file.getInputStream();
                     DigestInputStream digestStream = new DigestInputStream(raw, digest)) {
                    objectStorageService.upload(objectKey, digestStream, validated.fileSize(), validated.mimeType());
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("SHA-256 is not available", e);
        } catch (IOException e) {
            throw new StorageException("Unable to read the uploaded file", e);
        }
    }

    private void softDelete(FileStorage fileStorage) {
        fileStorage.setDeleted(true);
        fileStorage.setDeletedAt(LocalDateTime.now());
        fileStorageRepository.save(fileStorage);
    }

    private void deleteBinary(FileStorage fileStorage) {
        if (fileStorage.getObjectKey() != null && !fileStorage.getObjectKey().isBlank()) {
            try {
                objectStorageService.delete(fileStorage.getObjectKey());
            } catch (Exception e) {
                log.error("File delete failed fileId={} entityType={} entityId={} objectKey={}",
                        fileStorage.getId(), fileStorage.getEntityType(), fileStorage.getEntityId(),
                        fileStorage.getObjectKey(), e);
            }
        }
        deleteLegacyPhysicalFile(fileStorage.getFileName());
    }

    private void deleteStoredObjectQuietly(String objectKey) {
        try {
            objectStorageService.delete(objectKey);
        } catch (Exception e) {
            log.warn("Could not remove orphan object after metadata failure objectKey={}", objectKey, e);
        }
    }

    private boolean canCurrentUserAccess(FileStorage fileStorage) {
        try {
            assertCanAccess(fileStorage);
            return true;
        } catch (FileAccessDeniedException e) {
            return false;
        }
    }

    private void assertCanAccess(FileStorage fileStorage) {
        if (fileStorage.getEntityId() != null) {
            return;
        }
        assertOwnerOrAdmin(fileStorage);
    }

    private void assertCanModify(FileStorage fileStorage) {
        assertCanAccess(fileStorage);
    }

    private void assertOwnerOrAdmin(FileStorage fileStorage) {
        Optional<String> currentUser = SecurityUtils.getCurrentUserLogin();
        if (currentUser.isEmpty()) {
            return;
        }
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("EXECUTIVE")) {
            return;
        }
        String owner = fileStorage.getAddUserId();
        if (owner != null && !owner.isBlank() && !owner.equalsIgnoreCase(currentUser.get())) {
            throw new FileAccessDeniedException(getMessage("error.file.access_denied"));
        }
    }

    private Resource loadLegacyFilesystemResource(FileStorage fileStorage) {
        Path file = resolveExistingFilePath(fileStorage.getFileName());
        if (file == null || !Files.exists(file)) {
            throw new StoredFileNotFoundException(getMessage("error.file.not_found", fileStorage.getFileName()));
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new StorageException("File exists in DB but cannot be read from disk: " + fileStorage.getFileName());
        } catch (MalformedURLException e) {
            throw new StorageException("Error reading file: " + fileStorage.getFileName(), e);
        }
    }

    private boolean deleteLegacyPhysicalFile(String filename) {
        if (filename == null || filename.isBlank() || Filenames.containsPathTraversal(filename)) {
            return false;
        }
        Path file = resolveExistingFilePath(filename);
        if (file == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Failed to delete leftover filesystem file filename={}", filename, e);
            return false;
        }
    }

    private Path resolveExistingFilePath(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        Path mediaRoot = Paths.get(storageProperties.getMediaDir());
        Path uploadRoot = Paths.get(storageProperties.getUploadDir());
        Path[] candidates = new Path[]{
                mediaRoot.resolve("images").resolve(filename),
                mediaRoot.resolve("documents").resolve(filename),
                mediaRoot.resolve(filename),
                uploadRoot.resolve(filename)
        };
        for (Path candidate : candidates) {
            Path normalized = candidate.normalize().toAbsolutePath();
            if (Files.exists(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private Long extractIdFromApplicationUrl(String fileUrl) {
        for (String prefix : List.of(
                Constants.FILE_VIEW_URL_PREFIX,
                Constants.FILE_DOWNLOAD_URL_PREFIX,
                "/api/upload/")) {
            if (fileUrl.startsWith(prefix)) {
                String remainder = fileUrl.substring(prefix.length());
                if (remainder.matches("^\\d+$")) {
                    return Long.parseLong(remainder);
                }
            }
        }
        return null;
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl.startsWith(Constants.MEDIA_IMAGES_URL_PREFIX)) {
            return fileUrl.substring(Constants.MEDIA_IMAGES_URL_PREFIX.length());
        } else if (fileUrl.startsWith(Constants.MEDIA_DOCUMENTS_URL_PREFIX)) {
            return fileUrl.substring(Constants.MEDIA_DOCUMENTS_URL_PREFIX.length());
        } else if (fileUrl.startsWith(Constants.MEDIA_URL_PREFIX)) {
            return fileUrl.substring(Constants.MEDIA_URL_PREFIX.length());
        } else if (fileUrl.startsWith(Constants.UPLOADS_URL_PREFIX)) {
            return fileUrl.substring(Constants.UPLOADS_URL_PREFIX.length());
        }
        return fileUrl;
    }

    private static final class ObjectStorageResource extends InputStreamResource {
        private final FileStorage fileStorage;

        private ObjectStorageResource(InputStream inputStream, FileStorage fileStorage) {
            super(inputStream);
            this.fileStorage = fileStorage;
        }

        @Override
        public String getFilename() {
            return fileStorage.getOriginalName();
        }

        @Override
        public long contentLength() {
            return fileStorage.getFileSize() != null ? fileStorage.getFileSize() : -1L;
        }
    }
}
