package com.ozerler.marble.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Checks whether the configured RustFS URL accepts an HTTP connection.
 * A response, including 4xx, means the endpoint is up. Connection failures do not.
 */
@Component
@Slf4j
public class RustFsEndpointProbe implements RustFsBrowserAccess.Probe {

    static final Duration TIMEOUT = Duration.ofMillis(800);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public boolean canConnect(URI endpoint) {
        if (endpoint == null || endpoint.getHost() == null || endpoint.getHost().isBlank()) {
            return false;
        }
        URI probeUri = probeUri(endpoint);
        if (probeUri == null) {
            return false;
        }
        HttpRequest request = HttpRequest.newBuilder(probeUri)
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            boolean reachable = response.statusCode() < 500;
            if (!reachable) {
                log.debug("RustFS endpoint probe failed host={} status={}", endpoint.getHost(), response.statusCode());
            }
            return reachable;
        } catch (IOException ex) {
            log.debug("RustFS endpoint probe failed host={}: {}", endpoint.getHost(), ex.getMessage());
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.debug("RustFS endpoint probe interrupted host={}", endpoint.getHost());
            return false;
        }
    }

    private static URI probeUri(URI endpoint) {
        try {
            return new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(), "/", null, null);
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
