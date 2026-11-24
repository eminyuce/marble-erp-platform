package com.ozerler.marble.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.config.RateLimitProperties;
import com.ozerler.marble.dto.RateLimitSettings;
import com.ozerler.marble.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves the effective rate-limit configuration from YAML defaults plus
 * optional database overrides. The resolved snapshot is cached in memory and
 * evicted when an administrator saves or resets the values so changes apply
 * without a restart.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitSettingsService {

    private static final List<String> OVERRIDE_KEYS = List.of(
            Constants.SETTING_KEY_RATE_LIMITING,
            Constants.SETTING_KEY_RATE_LIMIT_LOGIN_MAX,
            Constants.SETTING_KEY_RATE_LIMIT_LOGIN_WINDOW,
            Constants.SETTING_KEY_RATE_LIMIT_FORM_MAX,
            Constants.SETTING_KEY_RATE_LIMIT_FORM_WINDOW,
            Constants.SETTING_KEY_RATE_LIMIT_GENERAL_MAX,
            Constants.SETTING_KEY_RATE_LIMIT_GENERAL_WINDOW
    );

    private final SettingService settingService;
    private final RateLimitProperties properties;
    private final AtomicReference<RateLimitSettings> cache = new AtomicReference<>();

    /**
     * YAML (and Java property) defaults. These remain the fallback when no
     * database override is stored.
     *
     * @return snapshot of the configured defaults (enabled defaults to true)
     */
    public RateLimitSettings yamlDefaults() {
        return new RateLimitSettings(
                true,
                properties.getLogin().getMaxRequests(),
                properties.getLogin().getWindowSeconds(),
                properties.getForm().getMaxRequests(),
                properties.getForm().getWindowSeconds(),
                properties.getGeneral().getMaxRequests(),
                properties.getGeneral().getWindowSeconds());
    }

    /**
     * Current effective values (defaults with database overrides applied).
     *
     * @return cached snapshot, loading from the settings store if needed
     */
    public RateLimitSettings effective() {
        RateLimitSettings cached = cache.get();
        if (cached != null) {
            return cached;
        }
        RateLimitSettings loaded = load();
        if (cache.compareAndSet(null, loaded)) {
            return loaded;
        }
        RateLimitSettings raced = cache.get();
        if (raced != null) {
            return raced;
        }
        return loaded;
    }

    /**
     * @return true when at least one rate-limit key has a database override
     */
    public boolean hasOverride() {
        for (String key : OVERRIDE_KEYS) {
            if (settingService.getSettingValue(key) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Drops the in-memory snapshot so the next {@link #effective()} call reloads.
     */
    public void evict() {
        cache.set(null);
    }

    /**
     * Validates and persists overrides, then evicts the cache so the new values
     * are used immediately.
     *
     * @param enabled whether rate limiting is active
     * @param loginMaxRequests login-tier max requests (raw form value)
     * @param loginWindowSeconds login-tier window in seconds
     * @param formMaxRequests form-tier max requests
     * @param formWindowSeconds form-tier window in seconds
     * @param generalMaxRequests general-tier max requests
     * @param generalWindowSeconds general-tier window in seconds
     */
    @Transactional
    public void save(boolean enabled,
                     String loginMaxRequests,
                     String loginWindowSeconds,
                     String formMaxRequests,
                     String formWindowSeconds,
                     String generalMaxRequests,
                     String generalWindowSeconds) {
        int loginMax = parsePositive(
                "Giriş — en fazla istek", loginMaxRequests,
                Constants.RATE_LIMIT_MAX_REQUESTS_MIN, Constants.RATE_LIMIT_MAX_REQUESTS_MAX);
        int loginWindow = parsePositive(
                "Giriş — pencere (saniye)", loginWindowSeconds,
                Constants.RATE_LIMIT_WINDOW_SECONDS_MIN, Constants.RATE_LIMIT_WINDOW_SECONDS_MAX);
        int formMax = parsePositive(
                "Form gönderimi — en fazla istek", formMaxRequests,
                Constants.RATE_LIMIT_MAX_REQUESTS_MIN, Constants.RATE_LIMIT_MAX_REQUESTS_MAX);
        int formWindow = parsePositive(
                "Form gönderimi — pencere (saniye)", formWindowSeconds,
                Constants.RATE_LIMIT_WINDOW_SECONDS_MIN, Constants.RATE_LIMIT_WINDOW_SECONDS_MAX);
        int generalMax = parsePositive(
                "Genel istekler — en fazla istek", generalMaxRequests,
                Constants.RATE_LIMIT_MAX_REQUESTS_MIN, Constants.RATE_LIMIT_MAX_REQUESTS_MAX);
        int generalWindow = parsePositive(
                "Genel istekler — pencere (saniye)", generalWindowSeconds,
                Constants.RATE_LIMIT_WINDOW_SECONDS_MIN, Constants.RATE_LIMIT_WINDOW_SECONDS_MAX);

        RateLimitSettings previous = effective();
        persist(Constants.SETTING_KEY_RATE_LIMITING, String.valueOf(enabled),
                "IP tabanlı istek sınırlandırmayı aç/kapat");
        persist(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_MAX, Integer.toString(loginMax),
                "Giriş katmanı: pencere başına en fazla istek");
        persist(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_WINDOW, Integer.toString(loginWindow),
                "Giriş katmanı: pencere süresi (saniye)");
        persist(Constants.SETTING_KEY_RATE_LIMIT_FORM_MAX, Integer.toString(formMax),
                "Form katmanı: pencere başına en fazla istek");
        persist(Constants.SETTING_KEY_RATE_LIMIT_FORM_WINDOW, Integer.toString(formWindow),
                "Form katmanı: pencere süresi (saniye)");
        persist(Constants.SETTING_KEY_RATE_LIMIT_GENERAL_MAX, Integer.toString(generalMax),
                "Genel katman: pencere başına en fazla istek");
        persist(Constants.SETTING_KEY_RATE_LIMIT_GENERAL_WINDOW, Integer.toString(generalWindow),
                "Genel katman: pencere süresi (saniye)");
        evict();
        RateLimitSettings next = effective();
        log.info("Rate limit settings updated by {}: {} -> {}", actor(), describe(previous), describe(next));
    }

    /**
     * Removes all rate-limit overrides so YAML defaults apply again, then evicts
     * the cache.
     */
    @Transactional
    public void resetToDefaults() {
        RateLimitSettings previous = effective();
        for (String key : OVERRIDE_KEYS) {
            settingService.deleteSetting(key);
        }
        evict();
        log.info("Rate limit settings reset to YAML defaults by {}: previous {}",
                actor(), describe(previous));
    }

    private RateLimitSettings load() {
        RateLimitSettings defaults = yamlDefaults();
        return new RateLimitSettings(
                readBoolean(Constants.SETTING_KEY_RATE_LIMITING, defaults.isEnabled()),
                readInt(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_MAX, defaults.getLoginMaxRequests()),
                readInt(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_WINDOW, defaults.getLoginWindowSeconds()),
                readInt(Constants.SETTING_KEY_RATE_LIMIT_FORM_MAX, defaults.getFormMaxRequests()),
                readInt(Constants.SETTING_KEY_RATE_LIMIT_FORM_WINDOW, defaults.getFormWindowSeconds()),
                readInt(Constants.SETTING_KEY_RATE_LIMIT_GENERAL_MAX, defaults.getGeneralMaxRequests()),
                readInt(Constants.SETTING_KEY_RATE_LIMIT_GENERAL_WINDOW, defaults.getGeneralWindowSeconds()));
    }

    private void persist(String key, String value, String description) {
        settingService.saveSetting(key, value, Constants.SETTING_CATEGORY_SECURITY, description);
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        String raw = settingService.getSettingValue(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim();
        if ("true".equalsIgnoreCase(normalized)
                || "1".equals(normalized)
                || "on".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)
                || "0".equals(normalized)
                || "off".equalsIgnoreCase(normalized)) {
            return false;
        }
        log.warn("Ignoring invalid boolean override for {}: {}", key, raw);
        return defaultValue;
    }

    private int readInt(String key, int defaultValue) {
        String raw = settingService.getSettingValue(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid integer override for {}: {}", key, raw);
            return defaultValue;
        }
    }

    private static int parsePositive(String label, String raw, int min, int max) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(label + " boş olamaz.");
        }
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " tam sayı olmalıdır.");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    label + " " + min + " ile " + max + " arasında olmalıdır.");
        }
        return value;
    }

    private static String actor() {
        return SecurityUtils.getCurrentUserLogin().orElse("unknown");
    }

    private static String describe(RateLimitSettings settings) {
        return "enabled=" + settings.isEnabled()
                + ", login=" + settings.getLoginMaxRequests() + "/" + settings.getLoginWindowSeconds()
                + ", form=" + settings.getFormMaxRequests() + "/" + settings.getFormWindowSeconds()
                + ", general=" + settings.getGeneralMaxRequests() + "/" + settings.getGeneralWindowSeconds();
    }
}
