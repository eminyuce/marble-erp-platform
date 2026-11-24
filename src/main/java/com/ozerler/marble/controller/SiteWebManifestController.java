package com.ozerler.marble.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves /site.webmanifest with the standard manifest MIME type, which some
 * servlet containers and reverse proxies do not infer from the file extension.
 */
@Controller
public class SiteWebManifestController {

    private static final MediaType MANIFEST_JSON =
            new MediaType("application", "manifest+json", StandardCharsets.UTF_8);

    /**
     * Returns the packaged web manifest.
     *
     * @return manifest JSON
     * @throws IOException if the packaged file cannot be read
     */
    @GetMapping("/site.webmanifest")
    @ResponseBody
    public ResponseEntity<String> manifest() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/site.webmanifest");
        String body = resource.getContentAsString(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MANIFEST_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(body);
    }
}
