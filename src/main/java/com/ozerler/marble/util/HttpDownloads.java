package com.ozerler.marble.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class HttpDownloads {

    private HttpDownloads() {
    }

    public static ResponseEntity<byte[]> attachment(byte[] body, String filename, MediaType contentType) {
        String encodedFilename = Urls.encode(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(contentType)
                .body(body);
    }
}
