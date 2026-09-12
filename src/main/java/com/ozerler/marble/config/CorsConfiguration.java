package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin:}")
    private String allowedOrigin;

    @Value("${app.cors.allowed-origin-pattern:}")
    private String allowedOriginPattern;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration mapping = registry.addMapping("/**")
                .allowedMethods(Constants.ALLOWED_CORS_METHODS)
                .allowedHeaders("*")
                .allowCredentials(true);

        String[] origins = splitCsv(allowedOrigin);
        if (origins.length > 0) {
            mapping.allowedOrigins(origins);
        }

        String[] patterns = splitCsv(allowedOriginPattern);
        if (patterns.length > 0) {
            mapping.allowedOriginPatterns(patterns);
        }
    }

    private static String[] splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .filter(part -> !"*".equals(part))
                .toArray(String[]::new);
    }
}
