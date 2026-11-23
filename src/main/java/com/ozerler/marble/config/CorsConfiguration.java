package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfiguration implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public CorsConfiguration(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration mapping = registry.addMapping("/**")
                .allowedMethods(Constants.ALLOWED_CORS_METHODS)
                .allowedHeaders("*")
                .allowCredentials(true);

        String[] origins = splitCsv(corsProperties.allowedOrigin());
        if (origins.length > 0) {
            mapping.allowedOrigins(origins);
        }

        String[] patterns = splitCsv(corsProperties.allowedOriginPattern());
        if (patterns.length > 0) {
            mapping.allowedOriginPatterns(patterns);
        }
    }

    private static String[] splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(value.split(","))
                .map(s -> s.trim())
                .filter(part -> !part.isEmpty())
                .filter(part -> !"*".equals(part))
                .toArray(String[]::new);
    }
}
