package com.ozerler.marble.storage;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class FileObjectKeyFactory {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp"
    );

    public String create(String entityType, Long entityId, String extension) {
        String entity = sanitizeEntityType(entityType);
        String entityFolder = entityId != null ? String.valueOf(entityId) : "pending";
        String ext = normalizeExtension(extension);
        String uniqueName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        if ("svg".equals(ext)) {
            return "svg/" + entity + "/" + entityFolder + "/" + uniqueName;
        }
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return "images/" + entity + "/" + entityFolder + "/" + uniqueName;
        }
        return "documents/" + entity + "/" + entityFolder + "/" + uniqueName;
    }

    private static String sanitizeEntityType(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return "general";
        }
        String normalized = entityType.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
        if ("blocks".equals(normalized)) {
            return "block";
        }
        return normalized.isBlank() ? "general" : normalized;
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "";
        }
        String cleaned = extension.startsWith(".") ? extension.substring(1) : extension;
        return cleaned.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
