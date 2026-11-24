package com.ozerler.marble.service;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ozerler.marble.dto.RateLimitSettings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    private final RateLimitSettingsService rateLimitSettingsService;

    /**
     * Last snapshot applied to the in-memory windows. When the effective
     * configuration changes, existing buckets are cleared so a tighter or
     * shorter window cannot be under- or over-counted.
     */
    private volatile RateLimitSettings lastApplied;

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
        RateLimitSettings settings = currentSettings();
        int maxRequests = maxRequests(settings, tier);
        int windowSeconds = windowSeconds(settings, tier);
        String key = clientIp + "|" + tier.name();
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Pencere dışındaki eski kayıtları temizle
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            log.warn("Rate limit exceeded — IP: {}, Tier: {}, Limit: {}/{} s",
                    clientIp, tier.name(), maxRequests, windowSeconds);
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

    private RateLimitSettings currentSettings() {
        RateLimitSettings now = rateLimitSettingsService.effective();
        RateLimitSettings previous = lastApplied;
        if (previous == null || !previous.equals(now)) {
            requestLog.clear();
            lastApplied = now;
        }
        return now;
    }

    private static int maxRequests(RateLimitSettings settings, Tier tier) {
        return switch (tier) {
            case LOGIN -> settings.getLoginMaxRequests();
            case FORM -> settings.getFormMaxRequests();
            case GENERAL -> settings.getGeneralMaxRequests();
        };
    }

    private static int windowSeconds(RateLimitSettings settings, Tier tier) {
        return switch (tier) {
            case LOGIN -> settings.getLoginWindowSeconds();
            case FORM -> settings.getFormWindowSeconds();
            case GENERAL -> settings.getGeneralWindowSeconds();
        };
    }

    private long getMaxWindowMillis() {
        RateLimitSettings settings = rateLimitSettingsService.effective();
        long maxSeconds = Math.max(
                settings.getLoginWindowSeconds(),
                Math.max(settings.getFormWindowSeconds(),
                        settings.getGeneralWindowSeconds()));
        return maxSeconds * 1000L;
    }

    /**
     * Window seconds currently applied to the given tier, used for Retry-After.
     *
     * @param tier the request tier
     * @return window length in seconds
     */
    public int windowSeconds(Tier tier) {
        return windowSeconds(currentSettings(), tier);
    }
}
