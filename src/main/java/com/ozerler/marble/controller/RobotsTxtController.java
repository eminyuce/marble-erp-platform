package com.ozerler.marble.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ozerler.marble.common.Constants;

/**
 * Serves /robots.txt. Crawling is allowed so that search engines can observe
 * the noindex / X-Robots-Tag signals; indexing itself is not permitted.
 */
@Controller
public class RobotsTxtController {

    /**
     * Returns the robots.txt body as UTF-8 text/plain with a one-day cache.
     *
     * @return robots.txt response
     */
    @GetMapping("/robots.txt")
    @ResponseBody
    public ResponseEntity<String> robotsTxt() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CACHE_CONTROL, Constants.ROBOTS_TXT_CACHE_CONTROL)
                .body(Constants.ROBOTS_TXT_BODY);
    }
}
