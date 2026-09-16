package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.FileStorage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStorageDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("originalName")
    private String originalName;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("fileSize")
    private Long fileSize;

    @JsonProperty("formattedSize")
    private String formattedSize;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("entityType")
    private String entityType;

    @JsonProperty("entityId")
    private Long entityId;

    @JsonProperty("image")
    private boolean image;

    @JsonProperty("pdf")
    private boolean pdf;

    @JsonProperty("word")
    private boolean word;

    @JsonProperty("text")
    private boolean text;

    @JsonProperty("extension")
    private String extension;

    @JsonProperty("createdDate")
    private LocalDateTime createdDate;

    public static FileStorageDto fromEntity(FileStorage entity) {
        if (entity == null) {
            return null;
        }
        return FileStorageDto.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .originalName(entity.getOriginalName())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .formattedSize(entity.getFormattedSize())
                .filePath(entity.getFilePath())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .image(entity.isImage())
                .pdf(entity.isPdf())
                .word(entity.isWord())
                .text(entity.isText())
                .extension(entity.getFileExtension())
                .createdDate(entity.getCreatedDate())
                .build();
    }
}
