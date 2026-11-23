package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * In-memory page window for grids whose rows are already loaded.
 */
public final class GridWindow {

    private GridWindow() {
    }

    public static int pageCount(int total, int size) {
        if (total <= 0 || size <= 0) {
            return 1;
        }
        return (total + size - 1) / size;
    }

    public static <T> List<T> page(List<T> rows, int page, int size) {
        if (rows == null || rows.isEmpty() || size <= 0) {
            return List.of();
        }
        int from = Math.min(Math.max(0, page - 1) * size, rows.size());
        int to = Math.min(from + size, rows.size());
        if (from >= to) {
            return List.of();
        }
        return rows.subList(from, to);
    }

    public static boolean matches(String search, String... parts) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.trim().toLowerCase(Locale.forLanguageTag("tr-TR"));
        StringBuilder haystack = new StringBuilder();
        if (parts != null) {
            for (String part : parts) {
                if (part != null && !part.isBlank()) {
                    haystack.append(' ').append(part);
                }
            }
        }
        return haystack.toString().toLowerCase(Locale.forLanguageTag("tr-TR")).contains(needle);
    }

    public static String clean(String value) {
        String text = GridTexts.text(value);
        if (text.equals("—") || text.equals("–") || text.equals("-")) {
            return "";
        }
        return text;
    }

    public static String withUnit(BigDecimal value, String unit) {
        String quantity = GridTexts.quantity(value);
        if (quantity.isBlank()) {
            return "";
        }
        return quantity + " " + unit;
    }

    public static String isoDate(String value) {
        String text = clean(value);
        if (text.isBlank()) {
            return "";
        }
        try {
            return GridTexts.date(LocalDate.parse(text));
        } catch (DateTimeParseException ex) {
            return text;
        }
    }
}
