package com.ozerler.marble.storage;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.config.ObjectStorageProperties;

class RustFsBrowserAccessTest {

    @Test
    @DisplayName("Docker service hostnames are not browser endpoints")
    void dockerServiceHostIsNotBrowserReachable() {
        RustFsBrowserAccess access = access("http://rustfs:9000", "", endpoint -> true);

        Assertions.assertThat(access.browserEndpoint()).isEmpty();
        Assertions.assertThat(access.canBrowserUsePresignedUrl("localhost")).isFalse();
        Assertions.assertThat(access.isBrowserSafeUrl(URI.create("http://rustfs:9000/erp-files/backups/file.sql.gz"))).isFalse();
    }

    @Test
    @DisplayName("blank public endpoint does not fall back to an internal endpoint")
    void blankPublicEndpointDoesNotExposeInternalHost() {
        RustFsBrowserAccess access = access("http://rustfs:9000", "  ", endpoint -> true);

        Assertions.assertThat(access.browserEndpoint()).isEmpty();
    }

    @Test
    @DisplayName("explicit public endpoint is used when the probe succeeds")
    void publicEndpointIsReachableWhenProbeSucceeds() {
        AtomicInteger probes = new AtomicInteger();
        RustFsBrowserAccess access = access("http://rustfs:9000", "https://files.example.com", endpoint -> {
            probes.incrementAndGet();
            Assertions.assertThat(endpoint.getHost()).isEqualTo("files.example.com");
            return true;
        });

        Assertions.assertThat(access.browserEndpoint())
                .hasValueSatisfying(uri -> Assertions.assertThat(uri.getHost()).isEqualTo("files.example.com"));
        Assertions.assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isTrue();
        Assertions.assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isTrue();
        Assertions.assertThat(probes.get()).isEqualTo(1);
        Assertions.assertThat(access.isBrowserSafeUrl(URI.create("https://files.example.com/erp-files/a.sql.gz?X-Amz-Signature=abc")))
                .isTrue();
        Assertions.assertThat(access.isBrowserSafeUrl(URI.create("memory://erp-files/a.sql.gz"))).isFalse();
    }

    @Test
    @DisplayName("public endpoint that cannot be reached is not offered to the browser")
    void unreachablePublicEndpointFallsBack() {
        RustFsBrowserAccess access = access("http://rustfs:9000", "https://files.example.com", endpoint -> false);

        Assertions.assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isFalse();
    }

    @Test
    @DisplayName("loopback RustFS is only offered when the browser is also on loopback")
    void loopbackEndpointRequiresLoopbackBrowser() {
        AtomicInteger probes = new AtomicInteger();
        RustFsBrowserAccess access = access("http://rustfs:9000", "http://127.0.0.1:9000", endpoint -> {
            probes.incrementAndGet();
            return true;
        });

        Assertions.assertThat(access.canBrowserUsePresignedUrl("erp.example.com")).isFalse();
        Assertions.assertThat(probes.get()).isZero();
        Assertions.assertThat(access.canBrowserUsePresignedUrl("localhost")).isTrue();
        Assertions.assertThat(probes.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("host.docker.internal is treated as an internal host")
    void dockerInternalHostIsRejected() {
        Assertions.assertThat(RustFsBrowserAccess.isInternalServiceHost("host.docker.internal")).isTrue();
        Assertions.assertThat(RustFsBrowserAccess.isInternalServiceHost("rustfs")).isTrue();
        Assertions.assertThat(RustFsBrowserAccess.isInternalServiceHost("files.example.com")).isFalse();
        Assertions.assertThat(RustFsBrowserAccess.isLoopback("localhost")).isTrue();
        Assertions.assertThat(RustFsBrowserAccess.isLoopback("127.0.0.1")).isTrue();
    }

    private static RustFsBrowserAccess access(String endpoint, String publicEndpoint, RustFsBrowserAccess.Probe probe) {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getRustfs().setEndpoint(endpoint);
        properties.getRustfs().setPublicEndpoint(publicEndpoint);
        properties.getRustfs().setBucket("erp-files");
        return new RustFsBrowserAccess(properties, probe);
    }
}
