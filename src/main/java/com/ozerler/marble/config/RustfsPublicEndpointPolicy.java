package com.ozerler.marble.config;

import com.ozerler.marble.storage.RustFsBrowserAccess;

import java.net.URI;

/**
 * Production browsers must reach RustFS through an HTTPS reverse proxy.
 * The internal Docker hostname is only for the application.
 */
public final class RustfsPublicEndpointPolicy {

    private RustfsPublicEndpointPolicy() {
    }

    public static void requireHttpsPublicHost(String publicEndpoint) {
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            throw new IllegalStateException(
                    "Production RUSTFS_PUBLIC_ENDPOINT is required and must be https.");
        }
        URI uri;
        try {
            uri = URI.create(publicEndpoint.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Production RUSTFS_PUBLIC_ENDPOINT is not a valid URI.", ex);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException(
                    "Production RUSTFS_PUBLIC_ENDPOINT must be https behind Nginx or another reverse proxy.");
        }
        String host = uri.getHost();
        if (host == null
                || RustFsBrowserAccess.isInternalServiceHost(host)
                || RustFsBrowserAccess.isLoopback(host)) {
            throw new IllegalStateException(
                    "Production RUSTFS_PUBLIC_ENDPOINT must be a public hostname, not an internal or loopback host.");
        }
    }
}
