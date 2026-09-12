package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Strings {

    private static final Locale TURKISH = Constants.LOCALE_TR;
    private static final Pattern PLACEHOLDER_PATTERN = Constants.PATTERN_TEMPLATE_PLACEHOLDER;

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

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String replacement = resolveVariable(key, variables);
            if (replacement == null) {
                replacement = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String resolveVariable(String key, Map<String, String> variables) {
        if (key == null || variables == null) {
            return null;
        }
        if (variables.containsKey(key)) {
            return variables.get(key);
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
