package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import com.ozerler.marble.util.Filenames;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp"
    );

    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of(
            "pdf", "docx", "doc"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "pdf", "docx", "doc"
    );

    @Value("${app.media.dir:${app.upload.dir:media}}")
    private String mediaDir;

    private final FileStorageRepository fileStorageRepository;
    private final org.springframework.context.MessageSource messageSource;

    private Path rootLocation;
    private Path imagesLocation;
    private Path documentsLocation;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(mediaDir);
        this.imagesLocation = this.rootLocation.resolve("images");
        this.documentsLocation = this.rootLocation.resolve("documents");

        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
            if (!Files.exists(imagesLocation)) {
                Files.createDirectories(imagesLocation);
            }
            if (!Files.exists(documentsLocation)) {
                Files.createDirectories(documentsLocation);
            }
        } catch (IOException e) {
            log.error("Could not initialize media storage directories: root={}, images={}, documents={}",
                    rootLocation, imagesLocation, documentsLocation, e);
            throw new IllegalStateException(getMessage("error.file.storage_init", rootLocation), e);
        }
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
     * Stores a file on the file system and persists its metadata in file_storage.
     * Separates images into media/images/ and documents into media/documents/.
     * Prefixes filename with page/entity (e.g. blocks_uuid.ext).
     */
    @Transactional
    public FileStorage storeFile(MultipartFile file, String entityType, Long entityId) throws IOException {
        Objects.requireNonNull(file, getMessage("error.file.required"));
        if (file.isEmpty()) {
            throw new IllegalArgumentException(getMessage("error.file.empty"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_file";
        }

        if (Filenames.containsPathTraversal(originalFilename)) {
            throw new IllegalArgumentException(getMessage("error.file.invalid_path", originalFilename));
        }

        String rawExtension = Filenames.extension(originalFilename);
        String cleanExtension = rawExtension.startsWith(".") ? rawExtension.substring(1).toLowerCase() : rawExtension.toLowerCase();

        if (!isAllowedExtension(cleanExtension)) {
            throw new IllegalArgumentException(getMessage("error.file.unsupported_type", cleanExtension));
        }

        String entity = (entityType != null && !entityType.isBlank()) ? entityType.trim().toLowerCase() : "blocks";
        String prefix = ("block".equals(entity) || "blocks".equals(entity)) ? "blocks" : entity;
        String uniqueFilename = prefix + "_" + UUID.randomUUID() + (rawExtension.isEmpty() ? "" : rawExtension);

        boolean isImage = isImageExtension(cleanExtension);
        Path destinationDir = isImage ? this.imagesLocation : this.documentsLocation;
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }

        Path destination = destinationDir.resolve(uniqueFilename).normalize().toAbsolutePath();
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = detectMimeType(cleanExtension);
        }

        String urlPrefix = isImage ? Constants.MEDIA_IMAGES_URL_PREFIX : Constants.MEDIA_DOCUMENTS_URL_PREFIX;
        String filePath = urlPrefix + uniqueFilename;

        FileStorage fileStorage = FileStorage.builder()
                .fileName(uniqueFilename)
                .originalName(originalFilename)
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .filePath(filePath)
                .entityType(entity.toUpperCase())
                .entityId(entityId)
                .deleted(false)
                .build();

        return fileStorageRepository.save(fileStorage);
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
        return fileStorageRepository.findByIdAndDeletedFalse(id);
    }

    @Transactional
    public void attachFilesToEntity(List<Long> fileIds, String entityType, Long entityId) {
        if (fileIds == null || fileIds.isEmpty() || entityId == null) {
            return;
        }
        String entity = entityType != null ? entityType.trim().toUpperCase() : "BLOCK";
        List<FileStorage> files = fileStorageRepository.findByIdInAndDeletedFalse(fileIds);
        for (FileStorage file : files) {
            file.setEntityType(entity);
            file.setEntityId(entityId);
        }
        fileStorageRepository.saveAll(files);
    }

    /**
     * Deletes a file record and removes the physical file from the file system.
     */
    @Transactional
    public boolean deleteFileRecord(Long fileId) {
        if (fileId == null) {
            return false;
        }

        Optional<FileStorage> fileOpt = fileStorageRepository.findById(fileId);
        if (fileOpt.isEmpty()) {
            return false;
        }

        FileStorage fileStorage = fileOpt.get();
        boolean physicalDeleted = deletePhysicalFile(fileStorage.getFileName());
        fileStorageRepository.delete(fileStorage);
        log.info("Deleted FileStorage id={} file={} (physical delete: {})", fileId, fileStorage.getFileName(), physicalDeleted);
        return true;
    }

    /**
     * Deletes all files linked to a specific entity, removing both database records
     * and physical files from the file system.
     */
    @Transactional
    public void deleteAllFilesForEntity(String entityType, Long entityId) {
        if (entityType == null || entityId == null) {
            return;
        }
        List<FileStorage> files = fileStorageRepository.findByEntityTypeAndEntityId(
                entityType.trim().toUpperCase(), entityId);
        for (FileStorage file : files) {
            deletePhysicalFile(file.getFileName());
        }
        if (!files.isEmpty()) {
            fileStorageRepository.deleteAll(files);
            log.info("Deleted {} files for entityType={} entityId={}", files.size(), entityType, entityId);
        }
    }

    /**
     * Deletes file physically by URL prefix and removes database record if found.
     */
    @Transactional
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null) {
            return false;
        }
        String filename = extractFilenameFromUrl(fileUrl);
        if (filename == null || filename.isBlank() || Filenames.containsPathTraversal(filename)) {
            log.warn("Invalid file deletion path rejected: {}", fileUrl);
            return false;
        }

        boolean physicalDeleted = deletePhysicalFile(filename);

        // Also clean up matching DB entry if exists
        try {
            var allMatching = fileStorageRepository.findAll().stream()
                    .filter(f -> filename.equals(f.getFileName()))
                    .toList();
            if (!allMatching.isEmpty()) {
                fileStorageRepository.deleteAll(allMatching);
            }
        } catch (Exception e) {
            log.warn("Could not remove DB record for deleted file url: {}", fileUrl, e);
        }

        return physicalDeleted;
    }

    /**
     * Loads a file as a Spring Resource for download or streaming.
     */
    public Resource loadAsResource(Long fileId) {
        FileStorage fileStorage = fileStorageRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.file.not_found", fileId)));

        Path file = resolveExistingFilePath(fileStorage.getFileName());
        if (file == null || !Files.exists(file)) {
            throw new IllegalStateException("File exists in DB but cannot be found on disk: " + fileStorage.getFileName());
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalStateException("File exists in DB but cannot be read from disk: " + fileStorage.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Error reading file: " + fileStorage.getFileName(), e);
        }
    }

    /**
     * System Health probe for media storage.
     * Verifies that both media/images and media/documents directories exist,
     * are readable, and are writable. Returns DOWN with detailed justification if any check fails.
     */
    public DependencyHealth checkStorageHealth() {
        // 1. Verify Images Directory
        DependencyHealth imagesCheck = verifyDirectoryAccess(imagesLocation, "Görseller (media/images)");
        if (!"UP".equals(imagesCheck.getStatus())) {
            return imagesCheck;
        }

        // 2. Verify Documents Directory
        DependencyHealth documentsCheck = verifyDirectoryAccess(documentsLocation, "Belgeler (media/documents)");
        if (!"UP".equals(documentsCheck.getStatus())) {
            return documentsCheck;
        }

        return DependencyHealth.up();
    }

    public String getImagesDirName() {
        return imagesLocation != null ? imagesLocation.toString() : "media/images";
    }

    public String getDocumentsDirName() {
        return documentsLocation != null ? documentsLocation.toString() : "media/documents";
    }

    private DependencyHealth verifyDirectoryAccess(Path dir, String label) {
        if (dir == null) {
            return DependencyHealth.down(label + " klasör yolu yapılandırılmamış");
        }

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            return DependencyHealth.down(label + " klasörü oluşturulamadı: " + e.getMessage());
        }

        if (!Files.isReadable(dir)) {
            return DependencyHealth.down(label + " klasöründe okuma izni yok (" + dir.toAbsolutePath() + ")");
        }

        if (!Files.isWritable(dir)) {
            return DependencyHealth.down(label + " klasöründe yazma izni yok (" + dir.toAbsolutePath() + ")");
        }

        // Active write and delete probe
        try {
            Path probe = Files.createTempFile(dir, ".health_probe_", ".tmp");
            Files.writeString(probe, "health-check-ok");
            Files.deleteIfExists(probe);
        } catch (Exception e) {
            return DependencyHealth.down(label + " klasörüne yazma testi başarısız: " + e.getMessage());
        }

        return DependencyHealth.up();
    }

    private boolean deletePhysicalFile(String filename) {
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
            log.error("Failed to delete physical file: {}", filename, e);
            return false;
        }
    }

    private Path resolveExistingFilePath(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        if (imagesLocation != null) {
            Path p = imagesLocation.resolve(filename).normalize().toAbsolutePath();
            if (Files.exists(p)) return p;
        }
        if (documentsLocation != null) {
            Path p = documentsLocation.resolve(filename).normalize().toAbsolutePath();
            if (Files.exists(p)) return p;
        }
        if (rootLocation != null) {
            Path p = rootLocation.resolve(filename).normalize().toAbsolutePath();
            if (Files.exists(p)) return p;
        }
        return imagesLocation != null ? imagesLocation.resolve(filename).normalize().toAbsolutePath() : null;
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

    private boolean isAllowedExtension(String extension) {
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    private boolean isImageExtension(String extension) {
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    private String detectMimeType(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }
}
