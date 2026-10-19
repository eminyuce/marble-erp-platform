package com.ozerler.marble.storage;

import com.ozerler.marble.config.ObjectStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RustFsBrowserAccessTest {

    @Test
    @DisplayName("Docker service hostnames are not browser endpoints")
    void dockerServiceHostIsNotBrowserReachable() {
        RustFsBrowserAccess access = access("http://rustfs:9000", "", endpoint -> true);

        assertThat(access.browserEndpoint()).isEmpty();
        assertThat(access.canBrowserUsePresignedUrl("localhost")).isFalse();
        assertThat(access.isBrowserSafeUrl(URI.create("http://rustfs:9000/erp-files/backups/file.sql.gz"))).isFalse();
    }

    @Test
    @DisplayName("blank public endpoint does not fall back to an internal endpoint")
    void blankPublicEndpointDoesNotExposeInternalHost() {
        RustFsBrowserAccess access = access("http://rustfs:9000", "  ", endpoint -> true);

        assertThat(access.browserEndpoint()).isEmpty();
    }

    @Test
    @DisplayName("explicit public endpoint is used when the probe succeeds")
    void publicEndpointIsReachableWhenProbeSucceeds() {
        AtomicInteger probes = new AtomicInteger();
        RustFsBrowserAccess access = access("http://rustfs:9000", "https://files.example.com", endpoint -> {
            probes.incrementAndGet();
            assertThat(endpoint.getHost()).isEqualTo("files.example.com");
            return true;
        });

        assertThat(access.browserEndpoint())
                .hasValueSatisfying(uri -> assertThat(uri.getHost()).isEqualTo("files.example.com"));
        assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isTrue();
        assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isTrue();
        assertThat(probes.get()).isEqualTo(1);
        assertThat(access.isBrowserSafeUrl(URI.create("https://files.example.com/erp-files/a.sql.gz?X-Amz-Signature=abc")))
                .isTrue();
        assertThat(access.isBrowserSafeUrl(URI.create("memory://erp-files/a.sql.gz"))).isFalse();
    }

    @Test
    @DisplayName("public endpoint that cannot be reached is not offered to the browser")
    void unreachablePublicEndpointFallsBack() {
        RustFsBrowserAccess access = access("http://rustfs:9000", "https://files.example.com", endpoint -> false);

        assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isFalse();
    }

    @Test
    @DisplayName("loopback RustFS is only offered when the browser is also on loopback")
    void loopbackEndpointRequiresLoopbackBrowser() {
        AtomicInteger probes = new AtomicInteger();
        RustFsBrowserAccess access = access("http://rustfs:9000", "http://127.0.0.1:9000", endpoint -> {
            probes.incrementAndGet();
            return true;
        });

        assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isFalse();
        assertThat(probes.get()).isZero();
        assertThat(access.canBrowserUsePresignedUrl("localhost")).isTrue();
        assertThat(probes.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("host.docker.internal is treated as an internal host")
    void dockerInternalHostIsRejected() {
        assertThat(RustFsBrowserAccess.isInternalServiceHost("host.docker.internal")).isTrue();
        assertThat(RustFsBrowserAccess.isInternalServiceHost("rustfs")).isTrue();
        assertThat(RustFsBrowserAccess.isInternalServiceHost("files.example.com")).isFalse();
        assertThat(RustFsBrowserAccess.isLoopback("localhost")).isTrue();
        assertThat(RustFsBrowserAccess.isLoopback("127.0.0.1")).isTrue();
    }

    private static RustFsBrowserAccess access(String endpoint, String publicEndpoint, RustFsBrowserAccess.Probe probe) {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getRustfs().setEndpoint(endpoint);
        properties.getRustfs().setPublicEndpoint(publicEndpoint);
        properties.getRustfs().setBucket("erp-files");
        return new RustFsBrowserAccess(properties, probe);
    }
}
