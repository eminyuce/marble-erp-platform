package com.ozerler.marble.storage;

import com.ozerler.marble.config.ObjectStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Decides whether a browser can fetch a RustFS presigned URL.
 * Backend calls always use the internal endpoint. A presigned URL is issued
 * only when the configured browser endpoint is a public or loopback host that
 * this process can actually reach, and never when that host is a Docker DNS name.
 */
@Component
@Slf4j
public class RustFsBrowserAccess {

    static final Duration PROBE_CACHE_TTL = Duration.ofSeconds(15);

    private final ObjectStorageProperties storageProperties;
    private final Probe probe;
    private final AtomicReference<ProbeCache> probeCache = new AtomicReference<>();

    public RustFsBrowserAccess(ObjectStorageProperties storageProperties, Probe probe) {
        this.storageProperties = storageProperties;
        this.probe = probe;
    }

    /**
     * @param requestHost host the user's browser used to reach this application;
     *                    loopback RustFS endpoints are usable only for a loopback request
     */
    public boolean canBrowserUsePresignedUrl(String requestHost) {
        Optional<URI> endpoint = browserEndpoint();
        if (endpoint.isEmpty()) {
            return false;
        }
        URI uri = endpoint.get();
        if (isLoopback(uri.getHost()) && !isLoopback(requestHost)) {
            return false;
        }
        boolean reachable = isReachable(uri);
        if (!reachable) {
            log.debug("RustFS browser endpoint is not reachable host={}", uri.getHost());
        }
        return reachable;
    }

    public boolean isBrowserSafeUrl(URI url) {
        if (url == null || !isHttp(url) || url.getHost() == null || isInternalServiceHost(url.getHost())) {
            return false;
        }
        return browserEndpoint()
                .map(endpoint -> endpoint.getHost().equalsIgnoreCase(url.getHost()))
                .orElse(false);
    }

    public Optional<URI> browserEndpoint() {
        ObjectStorageProperties.RustFs rustFs = storageProperties.getRustfs();
        String candidate = blankToNull(rustFs.getPublicEndpoint());
        if (candidate == null) {
            candidate = blankToNull(rustFs.getEndpoint());
        }
        if (candidate == null) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(candidate.trim());
            if (!isHttp(uri) || uri.getHost() == null || isInternalServiceHost(uri.getHost())) {
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (IllegalArgumentException ex) {
            log.warn("RustFS browser endpoint is not a valid URI");
            return Optional.empty();
        }
    }

    private boolean isReachable(URI endpoint) {
        String key = endpoint.getScheme() + "://" + endpoint.getHost() + ":" + portOf(endpoint);
        Instant now = Instant.now();
        ProbeCache cached = probeCache.get();
        if (cached != null && cached.endpoint().equals(key) && now.isBefore(cached.expiresAt())) {
            return cached.reachable();
        }
        boolean reachable = probe.canConnect(endpoint);
        probeCache.set(new ProbeCache(key, reachable, now.plus(PROBE_CACHE_TTL)));
        return reachable;
    }

    static boolean isInternalServiceHost(String host) {
        if (host == null || host.isBlank()) {
            return true;
        }
        String normalized = stripBrackets(host).toLowerCase(Locale.ROOT);
        if (isLoopback(normalized)) {
            return false;
        }
        if ("host.docker.internal".equals(normalized)) {
            return true;
        }
        if (!normalized.contains(".")) {
            return true;
        }
        return normalized.endsWith(".internal")
                || normalized.endsWith(".local")
                || normalized.endsWith(".docker");
    }

    static boolean isLoopback(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = stripBrackets(host).toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || normalized.startsWith("127.");
    }

    private static int portOf(URI endpoint) {
        if (endpoint.getPort() > 0) {
            return endpoint.getPort();
        }
        return "https".equalsIgnoreCase(endpoint.getScheme()) ? 443 : 80;
    }

    private static boolean isHttp(URI uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String stripBrackets(String host) {
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    /**
     * Short connectivity check against a configured RustFS endpoint. Implementations
     * must not follow redirects and must not send credentials.
     */
    @FunctionalInterface
    public interface Probe {
        boolean canConnect(URI endpoint);
    }

    private record ProbeCache(String endpoint, boolean reachable, Instant expiresAt) {
    }
}
