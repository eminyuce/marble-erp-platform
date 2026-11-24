package com.ozerler.marble.web;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.common.Constants;

class BrandAssetsResourceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("packaged icons, wordmark and a valid web manifest are present")
    void packagedAssetsAndValidManifestExist() throws Exception {
        String[] assets = {
                "static/favicon.ico",
                "static/apple-touch-icon.png",
                "static/icons/favicon-16x16.png",
                "static/icons/favicon-32x32.png",
                "static/icons/android-chrome-36x36.png",
                "static/icons/android-chrome-48x48.png",
                "static/icons/android-chrome-72x72.png",
                "static/icons/android-chrome-96x96.png",
                "static/icons/android-chrome-144x144.png",
                "static/icons/android-chrome-192x192.png",
                "static/icons/android-chrome-256x256.png",
                "static/icons/android-chrome-384x384.png",
                "static/icons/android-chrome-512x512.png",
                "static/icons/android-chrome-maskable-512x512.png",
                "static/brand/logo-ozerler.png",
                "static/brand/logo-ozerler@2x.png",
                "static/site.webmanifest"
        };
        for (String asset : assets) {
            ClassPathResource resource = new ClassPathResource(asset);
            Assertions.assertThat(resource.exists()).as(asset).isTrue();
            Assertions.assertThat(resource.contentLength()).as(asset).isGreaterThan(0);
        }

        String manifestJson;
        try (InputStream in = new ClassPathResource("static/site.webmanifest").getInputStream()) {
            manifestJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        JsonNode manifest = MAPPER.readTree(manifestJson);
        Assertions.assertThat(manifest.get("name").asText()).isEqualTo("Özerler Mermer ERP");
        Assertions.assertThat(manifest.get("short_name").asText()).isEqualTo("Özerler ERP");
        Assertions.assertThat(manifest.get("theme_color").asText()).isEqualTo(Constants.THEME_COLOR);
        Assertions.assertThat(manifest.get("background_color").asText()).isEqualTo("#ffffff");
        Assertions.assertThat(manifest.get("icons")).hasSize(3);
        Assertions.assertThat(manifest.get("icons").get(2).get("purpose").asText()).isEqualTo("maskable");
    }

    @Test
    @DisplayName("robots.txt body allows crawling and documents the noindex header")
    void robotsTxtBodyIsExact() {
        String expected = "User-agent: *\n"
                + "Allow: /\n"
                + "\n"
                + "# Indexing is blocked via noindex and the X-Robots-Tag header.\n";
        Assertions.assertThat(Constants.ROBOTS_TXT_BODY).isEqualTo(expected);
    }
}
