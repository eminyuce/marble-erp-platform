package com.ozerler.marble.util;

/**
 * Lenient enum parsing for request parameters.
 */
public final class Enums {

    private Enums() {
    }

    /**
     * Parses a request parameter into an enum constant. Blank or unknown values
     * return {@code null} so list filters can ignore a bad query string instead
     * of failing the request.
     *
     * @param type enum class
     * @param raw request value, may be {@code null}
     * @param <E> enum type
     * @return the matching constant, or {@code null}
     */
    public static <E extends Enum<E>> E parseOrNull(Class<E> type, String raw) {
        if (type == null || raw == null) {
            return null;
        }
        String name = raw.trim();
        if (name.isEmpty()) {
            return null;
        }
        E[] values = type.getEnumConstants();
        if (values == null) {
            return null;
        }
        for (E value : values) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
