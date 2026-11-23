package com.ozerler.marble.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reusable entity representing uploaded file metadata stored in the database.
 * Metadata for binaries stored in S3-compatible object storage (RustFS).
 * Associated with business entities using {@code entityType} and {@code entityId}.
 */
@Entity
@Table(name = "file_storage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class FileStorage extends AuditableEntity {

    private static final long KIB = 1024L;
    private static final long MIB = KIB * KIB;
    private static final long GIB = MIB * KIB;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "bucket_name", length = 100)
    private String bucketName;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Transient
    public boolean isImage() {
        if (mimeType != null && mimeType.toLowerCase().startsWith("image/")) {
            return true;
        }
        if (originalName != null) {
            String lower = originalName.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".png") || lower.endsWith(".webp")
                    || lower.endsWith(".gif") || lower.endsWith(".bmp")
                    || lower.endsWith(".svg");
        }
        return false;
    }

    @Transient
    public boolean isPdf() {
        if ("application/pdf".equalsIgnoreCase(mimeType)) {
            return true;
        }
        return originalName != null && originalName.toLowerCase().endsWith(".pdf");
    }

    @Transient
    public boolean isWord() {
        if (mimeType != null) {
            String lowerMime = mimeType.toLowerCase();
            if (lowerMime.contains("word") || lowerMime.contains("officedocument.wordprocessingml")) {
                return true;
            }
        }
        if (originalName != null) {
            String lower = originalName.toLowerCase();
            return lower.endsWith(".docx") || lower.endsWith(".doc");
        }
        return false;
    }

    @Transient
    public boolean isMarkdown() {
        if (originalName != null && originalName.toLowerCase().endsWith(".md")) {
            return true;
        }
        return mimeType != null && mimeType.toLowerCase().contains("markdown");
    }

    @Transient
    public boolean isText() {
        if (isMarkdown()) {
            return false;
        }
        if ("text/plain".equalsIgnoreCase(mimeType)) {
            return true;
        }
        return originalName != null && originalName.toLowerCase().endsWith(".txt");
    }

    @Transient
    public boolean isExcel() {
        if (mimeType != null) {
            String lowerMime = mimeType.toLowerCase();
            if (lowerMime.contains("excel") || lowerMime.contains("spreadsheetml")) {
                return true;
            }
        }
        if (originalName != null) {
            String lower = originalName.toLowerCase();
            return lower.endsWith(".xlsx") || lower.endsWith(".xls");
        }
        return false;
    }

    @Transient
    public boolean isCsv() {
        if (mimeType != null) {
            String lowerMime = mimeType.toLowerCase();
            if (lowerMime.contains("csv")) {
                return true;
            }
        }
        return originalName != null && originalName.toLowerCase().endsWith(".csv");
    }

    @Transient
    public boolean isDocument() {
        return isPdf() || isWord() || isText() || isMarkdown() || isExcel() || isCsv();
    }

    @Transient
    public String getFormattedSize() {
        if (fileSize == null || fileSize <= 0) {
            return "0 B";
        }
        if (fileSize < KIB) {
            return fileSize + " B";
        } else if (fileSize < MIB) {
            return String.format("%.1f KB", fileSize / (double) KIB);
        } else if (fileSize < GIB) {
            return String.format("%.2f MB", fileSize / (double) MIB);
        } else {
            return String.format("%.2f GB", fileSize / (double) GIB);
        }
    }

    @Transient
    public String getFileExtension() {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "";
    }
}
