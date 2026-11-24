package com.ozerler.marble.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Two-factor, login lockout and login CAPTCHA settings from {@code app.security.two-factor}.
 * The TOTP encryption key comes from {@code TOTP_ENCRYPTION_KEY_FILE} and is never logged.
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.two-factor")
@Getter
@Setter
public class TwoFactorProperties {

    private static final int DEFAULT_MAX_FAILURES = 5;
    private static final int DEFAULT_LOCK_MINUTES = 15;
    private static final int DEFAULT_PENDING_MINUTES = 10;
    private static final int DEFAULT_CAPTCHA_MINUTES = 3;

    /** Base64-encoded 32-byte AES-256 key. Blank only outside production. */
    private String encryptionKey = "";
    /** When true a missing or blank key stops startup. Production sets this. */
    private boolean keyRequired;
    /** Issuer shown in the authenticator app. */
    private String issuer = "Ozerler Mermer ERP";
    /** Failed logins allowed per client IP inside {@link #ipWindow}. */
    private int ipMaxFailures = DEFAULT_MAX_FAILURES;
    private Duration ipWindow = Duration.ofMinutes(1);
    /** Consecutive failed password or code attempts before the account is locked. */
    private int accountMaxFailures = DEFAULT_MAX_FAILURES;
    private Duration accountLockDuration = Duration.ofMinutes(DEFAULT_LOCK_MINUTES);
    /** How long a password-verified login may wait for the second factor. */
    private Duration pendingTimeout = Duration.ofMinutes(DEFAULT_PENDING_MINUTES);
    /** Failed logins in one browser session before the math question is shown. */
    private int captchaAfterFailures = 2;
    /** Lifetime of one math question. */
    private Duration captchaTtl = Duration.ofMinutes(DEFAULT_CAPTCHA_MINUTES);
}
