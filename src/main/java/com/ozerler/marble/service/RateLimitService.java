package com.ozerler.marble.service;

import com.ozerler.marble.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * IP tabanlı sliding-window istek sınırlandırma servisi.
 * Her IP + tier kombinasyonu için istek zaman damgalarını tutar.
 * Periyodik temizlik görevi süresi dolmuş kayıtları siler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    /**
     * İstek sınırlandırma katmanları — endpoint türüne göre farklı limitler uygulanır.
     */
    public enum Tier {
        LOGIN, FORM, GENERAL
    }

    private final RateLimitProperties properties;

    /**
     * Anahtar: "IP|TIER", Değer: istek zaman damgaları (epoch millis).
     */
    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    /**
     * Verilen IP ve katman için isteğe izin verilip verilmediğini kontrol eder.
     * İzin verilirse zaman damgasını kaydeder.
     *
     * @return true ise istek geçebilir, false ise limit aşılmış
     */
    public boolean isAllowed(String clientIp, Tier tier) {
        RateLimitProperties.TierConfig config = getTierConfig(tier);
        String key = clientIp + "|" + tier.name();
        long now = System.currentTimeMillis();
        long windowStart = now - (config.getWindowSeconds() * 1000L);

        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Pencere dışındaki eski kayıtları temizle
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= config.getMaxRequests()) {
            log.warn("Rate limit exceeded — IP: {}, Tier: {}, Limit: {}/{} s",
                    clientIp, tier.name(), config.getMaxRequests(), config.getWindowSeconds());
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    /**
     * Her 5 dakikada bir süresi dolmuş sliding window kayıtlarını temizler.
     */
    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        int removedKeys = 0;

        var iterator = requestLog.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Deque<Long> timestamps = entry.getValue();

            // Tier bilgisini key'den çıkar — en uzun pencere süresini kullan
            long maxWindowMillis = getMaxWindowMillis();
            long cutoff = now - maxWindowMillis;

            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            if (timestamps.isEmpty()) {
                iterator.remove();
                removedKeys++;
            }
        }

        if (removedKeys > 0) {
            log.debug("Rate limit cleanup: {} keys evicted, remaining: {}", removedKeys, requestLog.size());
        }
    }

    private RateLimitProperties.TierConfig getTierConfig(Tier tier) {
        return switch (tier) {
            case LOGIN -> properties.getLogin();
            case FORM -> properties.getForm();
            case GENERAL -> properties.getGeneral();
        };
    }

    private long getMaxWindowMillis() {
        long maxSeconds = Math.max(
                properties.getLogin().getWindowSeconds(),
                Math.max(properties.getForm().getWindowSeconds(),
                        properties.getGeneral().getWindowSeconds()));
        return maxSeconds * 1000L;
    }
}
