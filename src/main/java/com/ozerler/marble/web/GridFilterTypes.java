package com.ozerler.marble.web;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Resolves a persistable path to a filter kind and parses date/number inputs.
 * Date bounds use Europe/Istanbul; an inclusive end date covers that whole day.
 */
public final class GridFilterTypes {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private GridFilterTypes() {
    }

    public static GridFilterKind kind(Class<?> entityType, String relativePath) {
        return kindOf(javaType(entityType, relativePath));
    }

    public static GridFilterKind kindOf(Class<?> javaType) {
        if (isDate(javaType)) {
            return GridFilterKind.DATE;
        }
        if (isNumber(javaType)) {
            return GridFilterKind.NUMBER;
        }
        return GridFilterKind.TEXT;
    }

    public static Class<?> javaType(Class<?> entityType, String relativePath) {
        if (entityType == null || relativePath == null || relativePath.isBlank()) {
            return String.class;
        }
        Class<?> current = entityType;
        for (String part : relativePath.split("\\.")) {
            Field field = findField(current, part);
            if (field == null) {
                return String.class;
            }
            current = field.getType();
        }
        return current;
    }

    public static boolean isDate(Class<?> type) {
        if (type == null) {
            return false;
        }
        return LocalDate.class.equals(type)
                || LocalDateTime.class.equals(type)
                || Instant.class.equals(type)
                || OffsetDateTime.class.equals(type)
                || ZonedDateTime.class.equals(type)
                || Date.class.equals(type);
    }

    public static boolean isDateTime(Class<?> type) {
        return isDate(type) && !LocalDate.class.equals(type);
    }

    public static boolean isNumber(Class<?> type) {
        if (type == null) {
            return false;
        }
        return Number.class.isAssignableFrom(type)
                || type == int.class
                || type == long.class
                || type == short.class
                || type == byte.class
                || type == float.class
                || type == double.class;
    }

    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            return LocalDate.parse(trimmed, ISO_DATE);
        } catch (DateTimeParseException ex) {
            if (trimmed.length() >= 10 && trimmed.charAt(4) == '-' && trimmed.charAt(7) == '-') {
                try {
                    return LocalDate.parse(trimmed.substring(0, 10), ISO_DATE);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    public static BigDecimal parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim().replace(" ", "").replace(",", ".");
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    public static Instant startOfDayInstant(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    public static Instant startOfNextDayInstant(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                return field;
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
