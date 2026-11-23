package com.ozerler.marble.service;

import java.util.Locale;

/**
 * Hides setting values whose names look like credentials.
 */
public final class SettingValueMask {

    private SettingValueMask() {
    }

    public static String display(String key, String value) {
        String name = key == null ? "" : key.toLowerCase(Locale.ROOT);
        if (name.contains("password") || name.contains("secret") || name.contains("token") || name.contains("key")) {
            return "******";
        }
        return value;
    }
}
