package com.ozerler.marble.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErpFilesPathReservedTest {

    @Test
    @DisplayName("no application route is mapped to the RustFS path prefix")
    void controllersDoNotClaimErpFiles() throws IOException {
        List<String> hits = new ArrayList<>();
        try (var files = Files.walk(Path.of("src/main/java"))) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    if (source.contains("\"/erp-files") || source.contains("'/erp-files")) {
                        hits.add(path.toString());
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("nginx forwards /erp-files/ without rewriting the signed host or path")
    void nginxSnippetPreservesSigV4() throws IOException {
        String snippet = Files.readString(Path.of("deploy/nginx/ozerler.naklink.com-erp-files.conf"));
        assertThat(snippet).contains("location ^~ /erp-files/");
        assertThat(snippet).contains("proxy_pass http://127.0.0.1:9000;");
        assertThat(snippet).contains("proxy_set_header Host $host;");
        assertThat(snippet).doesNotContain("proxy_pass http://127.0.0.1:9000/;");
        assertThat(snippet).doesNotContain("rewrite");
    }
}
