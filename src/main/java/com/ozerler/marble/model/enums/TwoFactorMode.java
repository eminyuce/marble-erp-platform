package com.ozerler.marble.model.enums;

import java.util.Locale;

/**
 * System-wide two-factor mode stored in {@code system_settings.security.2fa.mode}.
 */
public enum TwoFactorMode {
    /** Nobody is asked for a second factor. This is the default. */
    DISABLED("2FA Kapalı"),
    /** Users with an enrolled authenticator app enter a 6-digit TOTP code after the password. */
    AUTHENTICATOR("Sadece Authenticator");

    private final String label;

    TwoFactorMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Parses a stored value. Unknown or blank values fall back to {@link #DISABLED}.
     *
     * @param value stored setting value
     * @return the parsed mode
     */
    public static TwoFactorMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DISABLED;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DISABLED;
        }
    }
}
