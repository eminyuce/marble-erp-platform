package com.ozerler.marble.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    private static final String[] ALLOWED_METHODS = {
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(parseAllowedOrigins())
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private String[] parseAllowedOrigins() {
        return Arrays.stream(allowedOrigin.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .filter(origin -> !"*".equals(origin))
                .toArray(String[]::new);
    }
}
