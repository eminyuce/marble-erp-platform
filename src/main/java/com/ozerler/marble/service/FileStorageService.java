package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.util.Filenames;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@lombok.RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final org.springframework.context.MessageSource messageSource;
    private Path rootLocation;

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
        this.rootLocation = Paths.get(uploadDir);
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
        } catch (IOException e) {
            log.error("Could not initialize storage directory: {}", rootLocation, e);
            throw new IllegalStateException(getMessage("error.file.storage_init", rootLocation), e);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        Objects.requireNonNull(file, getMessage("error.file.required"));
        if (file.isEmpty()) {
            throw new IllegalArgumentException(getMessage("error.file.empty"));
        }

        String originalFilename = file.getOriginalFilename();
        if (Filenames.containsPathTraversal(originalFilename)) {
            throw new IllegalArgumentException(getMessage("error.file.invalid_path", originalFilename));
        }

        String extension = Filenames.extension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + extension;
        Path destination = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return Constants.UPLOADS_URL_PREFIX + uniqueFilename;
    }

    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(Constants.UPLOADS_URL_PREFIX)) {
            return false;
        }
        String filename = fileUrl.substring(Constants.UPLOADS_URL_PREFIX.length());
        if (Filenames.containsPathTraversal(filename)) {
            log.warn("Invalid file deletion path rejected: {}", filename);
            return false;
        }

        Path file = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
            return false;
        }
    }
}
