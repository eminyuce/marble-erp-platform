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
public class StoredObjectMetadata {
    private String key;
    private long sizeBytes;
    private Instant lastModified;
    private String contentType;
}
