package com.ozerler.marble.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * IP tabanlı istek sınırlandırma (rate limiting) yapılandırma parametreleri.
 * application.yml içindeki "rate-limiting" altından okunur.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limiting")
@Getter
@Setter
public class RateLimitProperties {

    private static final int WINDOW_SECONDS = 60;
    private static final int GENERAL_REQUESTS_PER_WINDOW = 60;

    private TierConfig login = new TierConfig(5, WINDOW_SECONDS);
    private TierConfig form = new TierConfig(10, WINDOW_SECONDS);
    private TierConfig general = new TierConfig(GENERAL_REQUESTS_PER_WINDOW, WINDOW_SECONDS);

    @Getter
    @Setter
    public static class TierConfig {
        private int maxRequests;
        private int windowSeconds;

        public TierConfig() {
        }

        public TierConfig(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
