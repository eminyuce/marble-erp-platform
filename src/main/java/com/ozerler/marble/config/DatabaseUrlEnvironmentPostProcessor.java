package com.ozerler.marble.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes cloud provider database URLs (e.g., Render, Heroku) formatted as:
 * postgres://user:pass@host:port/dbname into Spring Boot JDBC format:
 * jdbc:postgresql://host:port/dbname with separate username and password.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            databaseUrl = environment.getProperty("DB_URL");
        }

        if (databaseUrl != null && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            try {
                String uriString = databaseUrl;
                if (uriString.startsWith("postgres://")) {
                    uriString = "postgresql://" + uriString.substring("postgres://".length());
                }

                URI uri = new URI(uriString);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath() != null ? uri.getPath() : "/marble_erp";

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                Map<String, Object> properties = new HashMap<>();
                properties.put("spring.datasource.url", jdbcUrl);

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    properties.put("spring.datasource.username", userInfo[0]);
                    if (userInfo.length > 1) {
                        properties.put("spring.datasource.password", userInfo[1]);
                    }
                }

                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrlProperties", properties));
            } catch (Exception ignored) {
                // If parsing fails, fall back to standard Spring datasource configuration
            }
        }
    }
}
