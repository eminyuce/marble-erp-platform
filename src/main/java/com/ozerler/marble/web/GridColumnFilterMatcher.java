package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;

/**
 * Applies header-filter operators to already loaded values (in-memory pages).
 */
public final class GridColumnFilterMatcher {

    private static final DateTimeFormatter TURKISH = DateTimeFormatter.ofPattern("d.M.uuuu");

    private GridColumnFilterMatcher() {
    }

    public static boolean matches(String actual, GridFilterOperator operator, String expected) {
        String left = normalize(actual);
        String right = normalize(expected);
        if (operator == GridFilterOperator.EQUALS) {
            return left.equals(right);
        }
        if (operator == GridFilterOperator.STARTS_WITH) {
            return left.startsWith(right);
        }
        if (operator == GridFilterOperator.NOT_CONTAINS) {
            return !left.contains(right);
        }
        return left.contains(right);
    }

    public static boolean matches(GridColumnFilters filters, Function<String, String> values) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (GridColumnFilter filter : filters.getItems()) {
            String actual;
            if (values == null) {
                actual = "";
            } else {
                actual = values.apply(filter.getKey());
            }
            if (!matchesValue(actual, filter)) {
                return false;
            }
        }
        return true;
    }

    public static boolean matchesValue(String actual, GridColumnFilter filter) {
        GridFilterOperator operator = filter.getOperator();
        if (operator == GridFilterOperator.BETWEEN
                || operator == GridFilterOperator.BEFORE
                || operator == GridFilterOperator.AFTER
                || GridFilterTypes.parseDate(filter.getValue()) != null && parseFlexibleDate(actual) != null) {
            LocalDate expected = GridFilterTypes.parseDate(filter.getValue());
            LocalDate value = parseFlexibleDate(actual);
            if (expected != null && value != null) {
                return matchesDate(value, filter, expected);
            }
        }
        if ((operator == GridFilterOperator.BETWEEN || operator == GridFilterOperator.EQUALS)
                && GridFilterTypes.parseNumber(filter.getValue()) != null
                && GridFilterTypes.parseNumber(actual) != null
                && parseFlexibleDate(actual) == null) {
            return matchesNumber(actual, filter);
        }
        return matches(actual, operator, filter.getValue());
    }

    private static boolean matchesDate(LocalDate actual, GridColumnFilter filter, LocalDate from) {
        GridFilterOperator operator = filter.getOperator();
        if (operator == GridFilterOperator.BEFORE) {
            return actual.isBefore(from);
        }
        if (operator == GridFilterOperator.AFTER) {
            return actual.isAfter(from);
        }
        if (operator == GridFilterOperator.BETWEEN) {
            LocalDate to = GridFilterTypes.parseDate(filter.getToValue());
            if (to == null) {
                return false;
            }
            return !actual.isBefore(from) && !actual.isAfter(to);
        }
        return actual.equals(from);
    }

    private static boolean matchesNumber(String actual, GridColumnFilter filter) {
        BigDecimal value = GridFilterTypes.parseNumber(actual);
        BigDecimal from = GridFilterTypes.parseNumber(filter.getValue());
        if (value == null || from == null) {
            return false;
        }
        if (filter.getOperator() == GridFilterOperator.BETWEEN) {
            BigDecimal to = GridFilterTypes.parseNumber(filter.getToValue());
            if (to == null) {
                return false;
            }
            return value.compareTo(from) >= 0 && value.compareTo(to) <= 0;
        }
        return value.compareTo(from) == 0;
    }

    private static LocalDate parseFlexibleDate(String raw) {
        LocalDate iso = GridFilterTypes.parseDate(raw);
        if (iso != null) {
            return iso;
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), TURKISH);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
