package com.ozerler.marble.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring Cache abstraction configuration.
 * Caches frequently queried reference/configuration data (settings, email templates, quarries, cost centers).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_SETTINGS = "settings";
    public static final String CACHE_EMAIL_TEMPLATES = "emailTemplates";
    public static final String CACHE_QUARRIES = "quarries";
    public static final String CACHE_COST_CENTERS = "costCenters";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
                CACHE_SETTINGS,
                CACHE_EMAIL_TEMPLATES,
                CACHE_QUARRIES,
                CACHE_COST_CENTERS
        ));
        return cacheManager;
    }
}
