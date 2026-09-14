package com.ozerler.marble.model.enums;

import java.util.function.Function;

/**
 * Resolves a stored enum-name code to a localized label without changing the
 * persisted value.
 */
final class NamedEnumLabels {

    private NamedEnumLabels() {
    }

    static <E extends Enum<E>> String labelOf(Class<E> type, String code, Function<E, String> label) {
        if (code == null || code.isBlank()) {
            return "";
        }
        String normalized = code.trim();
        for (E value : type.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(normalized)) {
                return label.apply(value);
            }
        }
        return normalized;
    }
}
