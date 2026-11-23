package com.ozerler.marble.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupItemDto {
    private String fileName;
    private String s3Key;
    private long sizeBytes;
    private String formattedSize;
    private Instant createdAt;
    private String downloadUrl;
}
