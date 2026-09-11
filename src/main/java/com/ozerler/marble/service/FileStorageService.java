package com.ozerler.marble.service;

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
public class FileStorageService {

    private static final String UPLOADS_PREFIX = "/uploads/";

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir);
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
        } catch (IOException e) {
            log.error("Could not initialize storage directory: {}", rootLocation, e);
            throw new IllegalStateException("Depolama dizini başlatılamadı: " + rootLocation, e);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        Objects.requireNonNull(file, "Yüklenecek dosya null olamaz");
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Boş dosya kaydedilemez");
        }

        String originalFilename = file.getOriginalFilename();
        if (Filenames.containsPathTraversal(originalFilename)) {
            throw new IllegalArgumentException("Geçersiz dosya yolu tespit edildi: " + originalFilename);
        }

        String extension = Filenames.extension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + extension;
        Path destination = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return UPLOADS_PREFIX + uniqueFilename;
    }

    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(UPLOADS_PREFIX)) {
            return false;
        }
        String filename = fileUrl.substring(UPLOADS_PREFIX.length());
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
