package com.ozerler.marble.service;

import com.ozerler.marble.util.Ints;

/**
 * How long success, error, and field-check notices stay on screen.
 * Stored as whole seconds in {@code system_settings}.
 */
public final class NoticeDismissDuration {

    public static final String SETTING_KEY = "ui.notice.dismiss_seconds";
    public static final int DEFAULT_SECONDS = 100;
    public static final int MINIMUM_SECONDS = 1;
    public static final int MAXIMUM_SECONDS = 3600;

    private NoticeDismissDuration() {
    }

    public static boolean isAcceptable(String raw) {
        return secondsOrNull(raw) != null;
    }

    public static int seconds(String raw) {
        Integer parsed = secondsOrNull(raw);
        return parsed != null ? parsed : DEFAULT_SECONDS;
    }

    private static Integer secondsOrNull(String raw) {
        Integer parsed = parse(raw);
        if (parsed == null || parsed < MINIMUM_SECONDS || parsed > MAXIMUM_SECONDS) {
            return null;
        }
        return parsed;
    }

    private static Integer parse(String raw) {
        try {
            return Ints.parseLenientOrNull(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
