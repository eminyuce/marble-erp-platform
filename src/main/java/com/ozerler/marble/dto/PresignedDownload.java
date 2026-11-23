package com.ozerler.marble.dto;

import java.time.Instant;

/**
 * A one-time browser link. Callers generate this per response and do not persist it.
 */
public record PresignedDownload(String url, long expiresInSeconds, Instant expiresAt) {
}
