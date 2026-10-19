package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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
