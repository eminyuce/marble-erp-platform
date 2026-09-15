package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Builds ASCII-safe export/download filenames in the form
 * {@code entity_yyyy-MM-dd_HH-mm-ss.ext} using the JVM default timezone.
 */
public final class ExportFilenames {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT_EXPORT_FILENAME);

    private ExportFilenames() {
    }

    public static String build(String entityName, String extension) {
        return build(entityName, extension, LocalDateTime.now(ZoneId.systemDefault()));
    }

    public static String build(String entityName, String extension, LocalDateTime timestamp) {
        LocalDateTime effectiveTimestamp = timestamp == null
                ? LocalDateTime.now(ZoneId.systemDefault())
                : timestamp;
        String stem = entityStem(entityName);
        String ext = normalizeExtension(extension);
        return stem + "_" + effectiveTimestamp.format(TIMESTAMP) + ext;
    }

    private static String entityStem(String entityName) {
        String ascii = TurkishAsciiFilename.toAsciiTurkishFilename(entityName);
        int lastDot = ascii.lastIndexOf('.');
        if (lastDot > 0) {
            String withoutExtension = ascii.substring(0, lastDot);
            return withoutExtension.isBlank() ? Constants.FALLBACK_DOWNLOAD_FILENAME : withoutExtension;
        }
        return ascii;
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "";
        }
        String trimmed = extension.trim();
        while (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isBlank()) {
            return "";
        }
        String ascii = TurkishAsciiFilename.toAsciiTurkishFilename(trimmed);
        return ascii.isEmpty() ? "" : "." + ascii;
    }
}
