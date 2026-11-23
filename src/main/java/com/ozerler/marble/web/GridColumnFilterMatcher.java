package com.ozerler.marble.web;

import java.util.Locale;
import java.util.function.Function;

/**
 * Applies header-filter operators to already loaded text values (in-memory pages).
 */
public final class GridColumnFilterMatcher {

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
            if (!matches(actual, filter.getOperator(), filter.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
