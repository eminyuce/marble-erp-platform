package com.ozerler.marble.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * IP tabanlı istek sınırlandırma (rate limiting) yapılandırma parametreleri.
 * application.yml içindeki "rate-limiting" altından okunur.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limiting")
@Getter
@Setter
public class RateLimitProperties {

    private TierConfig login = new TierConfig(5, 60);
    private TierConfig form = new TierConfig(10, 60);
    private TierConfig general = new TierConfig(60, 60);

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
