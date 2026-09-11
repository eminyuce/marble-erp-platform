package com.ozerler.marble.util;

import java.util.Locale;
import java.util.Map;

public final class Strings {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private Strings() {
    }

    public static String lowerTurkish(String value) {
        return value == null ? "" : value.toLowerCase(TURKISH);
    }

    public static String joinDistinct(String separator, String first, String second) {
        boolean hasFirst = isPresent(first);
        boolean hasSecond = isPresent(second);
        if (!hasFirst) {
            return hasSecond ? second : null;
        }
        if (!hasSecond || second.equals(first)) {
            return first;
        }
        return first + separator + second;
    }

    public static String replacePlaceholders(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String replacement = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace("{{" + entry.getKey() + "}}", replacement);
        }
        return rendered;
    }

    public static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
