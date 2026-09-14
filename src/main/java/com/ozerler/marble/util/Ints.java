package com.ozerler.marble.util;

public final class Ints {

    private Ints() {
    }

    public static int parseOrDefault(String value, int defaultValue) {
        try {
            Integer parsed = parseLenientOrNull(value);
            return parsed != null ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Parses integers from query strings. Tabulator can send {@code undefined} or
     * {@code null} as the literal text of {@code page}/{@code size}; those must
     * become {@code null} so {@code @RequestParam(defaultValue)} can apply.
     */
    public static Integer parseLenientOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return Integer.valueOf(trimmed);
    }
}
