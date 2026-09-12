package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
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


    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
                Constants.CACHE_SETTINGS,
                Constants.CACHE_EMAIL_TEMPLATES,
                Constants.CACHE_QUARRIES,
                Constants.CACHE_COST_CENTERS
        ));
        return cacheManager;
    }
}
