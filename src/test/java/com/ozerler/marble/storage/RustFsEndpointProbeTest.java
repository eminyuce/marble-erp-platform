package com.ozerler.marble.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class RustFsEndpointProbeTest {

    @Test
    @DisplayName("probe succeeds when the endpoint accepts HTTP")
    void probeSucceedsWhenEndpointAcceptsHttp() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            assertThat(new RustFsEndpointProbe().canConnect(endpoint)).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("probe fails when nothing is listening")
    void probeFailsWhenEndpointIsDown() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        URI endpoint = URI.create("http://127.0.0.1:" + closedPort);
        assertThat(new RustFsEndpointProbe().canConnect(endpoint)).isFalse();
    }
}
