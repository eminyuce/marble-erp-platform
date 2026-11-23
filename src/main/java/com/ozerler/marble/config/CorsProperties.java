package com.ozerler.marble.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call this application. Bound from {@code app.cors}.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(String allowedOrigin, String allowedOriginPattern) {

    public CorsProperties {
        if (allowedOrigin == null) {
            allowedOrigin = "";
        }
        if (allowedOriginPattern == null) {
            allowedOriginPattern = "";
        }
    }
}
