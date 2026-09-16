package com.ozerler.marble.validation;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.exception.FileValidationException;
import com.ozerler.marble.util.Filenames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
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
            "pdf", "docx", "doc", "txt", "xlsx", "xls", "csv"
    );

    private static final Set<String> ALLOWED_EXTENSIONS;

    static {
        Set<String> extensions = new HashSet<>();
        extensions.addAll(ALLOWED_IMAGE_EXTENSIONS);
        extensions.addAll(ALLOWED_DOCUMENT_EXTENSIONS);
        ALLOWED_EXTENSIONS = Set.copyOf(extensions);
    }

    private static final Map<String, Set<String>> ALLOWED_MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("bmp", Set.of("image/bmp")),
            Map.entry("svg", Set.of("image/svg+xml", "image/svg", "text/xml", "application/xml", "text/plain")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/zip")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/zip")),
            Map.entry("xls", Set.of("application/vnd.ms-excel", "application/octet-stream")),
            Map.entry("csv", Set.of("text/csv", "application/csv", "text/plain", "text/x-csv")),
            Map.entry("txt", Set.of("text/plain"))
    );

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

        String declaredMime = file.getContentType();
        if (declaredMime != null && !declaredMime.isBlank() && !"application/octet-stream".equalsIgnoreCase(declaredMime)) {
            String mimeBase = declaredMime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            Set<String> allowedMimes = ALLOWED_MIME_BY_EXTENSION.getOrDefault(cleanExtension, Set.of());
            if (!allowedMimes.contains(mimeBase)) {
                throw new FileValidationException("File content type does not match the file extension");
            }
        }

        byte[] header = readHeader(file, 16);
        validateSignature(cleanExtension, header);

        byte[] svgBytes = null;
        if ("svg".equals(cleanExtension)) {
            svgBytes = readAll(file);
            svgContentValidator.validate(svgBytes);
        }

        String mimeType = resolveMimeType(cleanExtension, declaredMime);
        return new ValidatedUpload(originalFilename, rawExtension, cleanExtension, mimeType, file.getSize(), svgBytes);
    }

    public static boolean isImageExtension(String extension) {
        return extension != null && ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    public static String detectMimeType(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }

    private static String resolveMimeType(String extension, String declaredMime) {
        if (declaredMime != null && !declaredMime.isBlank() && !"application/octet-stream".equalsIgnoreCase(declaredMime)) {
            return declaredMime.split(";", 2)[0].trim();
        }
        return detectMimeType(extension);
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
            case "svg", "txt", "csv", "doc", "xls" -> {
                // Textual or legacy binary formats without a strict short magic header.
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

    private static byte[] readHeader(MultipartFile file, int length) {
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
