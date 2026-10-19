package com.ozerler.marble.validation;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.exception.FileValidationException;
import com.ozerler.marble.util.Filenames;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FileUploadValidator {

    static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg"
    );

    static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of(
            "pdf", "docx", "doc", "txt", "md", "xlsx", "xls", "csv"
    );

    /**
     * How many leading bytes are enough for Tika's magic-byte detector.
     */
    public static final int MEDIA_TYPE_PREFIX_BYTES = 64 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS;

    static {
        Set<String> extensions = new HashSet<>();
        extensions.addAll(ALLOWED_IMAGE_EXTENSIONS);
        extensions.addAll(ALLOWED_DOCUMENT_EXTENSIONS);
        ALLOWED_EXTENSIONS = Set.copyOf(extensions);
    }

    /**
     * Types Tika may report for an extension. The first entry is the type stored
     * on the file; the rest are acceptable aliases and generic parents. Tika does
     * not apply this allowlist, and the client Content-Type header is ignored.
     */
    private static final Map<String, List<String>> PERMITTED_MEDIA_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", List.of("image/jpeg")),
            Map.entry("jpeg", List.of("image/jpeg")),
            Map.entry("png", List.of("image/png")),
            Map.entry("webp", List.of("image/webp")),
            Map.entry("gif", List.of("image/gif")),
            Map.entry("bmp", List.of("image/bmp")),
            Map.entry("svg", List.of("image/svg+xml", "image/svg", "text/xml", "application/xml", "text/plain")),
            Map.entry("pdf", List.of("application/pdf")),
            Map.entry("docx", List.of(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/x-tika-ooxml",
                    "application/zip")),
            Map.entry("doc", List.of("application/msword", "application/x-tika-msoffice")),
            Map.entry("xlsx", List.of(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/x-tika-ooxml",
                    "application/zip")),
            Map.entry("xls", List.of("application/vnd.ms-excel", "application/x-tika-msoffice")),
            Map.entry("csv", List.of("text/csv", "application/csv", "text/plain", "text/x-csv")),
            Map.entry("txt", List.of("text/plain")),
            Map.entry("md", List.of("text/markdown", "text/x-web-markdown", "text/x-markdown", "text/plain"))
    );

    private static final Tika TIKA = new Tika();

    private final ObjectStorageProperties storageProperties;
    private final SvgContentValidator svgContentValidator;

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null) {
            throw new FileValidationException("Uploaded file cannot be null");
        }
        if (file.isEmpty()) {
            throw new FileValidationException("Cannot save empty file");
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new FileValidationException("File exceeds the maximum allowed size");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_file";
        }
        originalFilename = originalFilename.trim();
        if (Filenames.containsPathTraversal(originalFilename) || originalFilename.indexOf('\0') >= 0) {
            throw new FileValidationException("Invalid file path detected: " + originalFilename);
        }

        String rawExtension = Filenames.extension(originalFilename);
        String cleanExtension = rawExtension.startsWith(".")
                ? rawExtension.substring(1).toLowerCase(Locale.ROOT)
                : rawExtension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(cleanExtension)) {
            throw new FileValidationException("Unsupported file type: " + cleanExtension);
        }

        byte[] content = readContent(file, cleanExtension);
        String detectedMimeType = detectMimeType(originalFilename, content);
        requirePermittedMediaType(cleanExtension, detectedMimeType);
        validateSignature(cleanExtension, content);

        byte[] svgBytes = null;
        if ("svg".equals(cleanExtension)) {
            svgContentValidator.validate(content);
            svgBytes = content;
        }

        String mimeType = preferredMediaType(cleanExtension);
        return new ValidatedUpload(originalFilename, rawExtension, cleanExtension, mimeType, file.getSize(), svgBytes);
    }

    public static boolean isImageExtension(String extension) {
        return extension != null && ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    public static String detectMimeType(String filename, byte[] content) {
        byte[] prefix = content == null ? new byte[0] : content;
        String detected = filename == null || filename.isBlank()
                ? TIKA.detect(prefix)
                : TIKA.detect(prefix, filename);
        return MediaType.parse(detected).getBaseType().toString();
    }

    private static void requirePermittedMediaType(String extension, String detectedMimeType) {
        List<String> permitted = PERMITTED_MEDIA_TYPES_BY_EXTENSION.getOrDefault(extension, List.of());
        if (!permitted.contains(detectedMimeType)) {
            throw new FileValidationException("File content type does not match the file extension");
        }
    }

    private static String preferredMediaType(String extension) {
        List<String> permitted = PERMITTED_MEDIA_TYPES_BY_EXTENSION.get(extension);
        if (permitted == null || permitted.isEmpty()) {
            throw new FileValidationException("Unsupported file type: " + extension);
        }
        return permitted.get(0);
    }

    private static void validateSignature(String extension, byte[] header) {
        if (header.length == 0) {
            throw new FileValidationException("Cannot save empty file");
        }
        switch (extension) {
            case "jpg", "jpeg" -> requireMagic(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "JPEG");
            case "png" -> requireMagic(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "PNG");
            case "gif" -> requireAsciiPrefix(header, "GIF8", "GIF");
            case "bmp" -> requireAsciiPrefix(header, "BM", "BMP");
            case "webp" -> {
                requireAsciiPrefix(header, "RIFF", "WEBP");
                if (header.length >= 12) {
                    String webp = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
                    if (!"WEBP".equals(webp)) {
                        throw new FileValidationException("File content does not match the WEBP type");
                    }
                }
            }
            case "pdf" -> requireAsciiPrefix(header, "%PDF", "PDF");
            case "docx", "xlsx" -> requireMagic(header, new byte[]{0x50, 0x4B}, "Office Open XML");
            case "svg", "txt", "md", "csv", "doc", "xls" -> {
                // SVG is checked by SvgContentValidator. Text and legacy OLE
                // documents have no short magic header beyond Tika's detection.
            }
            default -> {
            }
        }
    }

    private static void requireMagic(byte[] header, byte[] expected, String type) {
        if (header.length < expected.length) {
            throw new FileValidationException("File content does not match the " + type + " type");
        }
        for (int i = 0; i < expected.length; i++) {
            if (header[i] != expected[i]) {
                throw new FileValidationException("File content does not match the " + type + " type");
            }
        }
    }

    private static void requireAsciiPrefix(byte[] header, String prefix, String type) {
        if (header.length < prefix.length()) {
            throw new FileValidationException("File content does not match the " + type + " type");
        }
        String actual = new String(header, 0, prefix.length(), java.nio.charset.StandardCharsets.US_ASCII);
        if (!prefix.equals(actual)) {
            throw new FileValidationException("File content does not match the " + type + " type");
        }
    }

    private static byte[] readContent(MultipartFile file, String extension) {
        if ("svg".equals(extension)) {
            return readAll(file);
        }
        return readPrefix(file, MEDIA_TYPE_PREFIX_BYTES);
    }

    private static byte[] readPrefix(MultipartFile file, int length) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(length);
        } catch (IOException e) {
            throw new FileValidationException("Unable to read the uploaded file");
        }
    }

    private static byte[] readAll(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new FileValidationException("Unable to read the uploaded file");
        }
    }

    public record ValidatedUpload(
            String originalFilename,
            String rawExtension,
            String cleanExtension,
            String mimeType,
            long fileSize,
            byte[] svgBytes
    ) {
    }
}
