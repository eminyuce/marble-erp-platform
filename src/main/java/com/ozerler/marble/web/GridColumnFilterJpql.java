package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds a JPQL predicate for active header filters, inlining escaped literals (no extra bind params).
 */
public final class GridColumnFilterJpql {

    private static final Pattern PATH_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    private GridColumnFilterJpql() {
    }

    public static String predicate(String rootAlias, Map<String, String> aliases, GridColumnFilters filters) {
        if (filters == null || filters.isEmpty() || !isSafePath(rootAlias)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (GridColumnFilter filter : filters.getItems()) {
            String path = propertyPath(rootAlias, aliases, filter.getKey());
            if (path == null) {
                continue;
            }
            String clause = clause(path, filter);
            if (!clause.isBlank()) {
                parts.add(clause);
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        return "(" + String.join(" AND ", parts) + ")";
    }

    static String propertyPath(String rootAlias, Map<String, String> aliases, String key) {
        if (aliases == null || key == null) {
            return null;
        }
        String relative = aliases.get(key);
        if (relative == null || relative.isBlank() || !isSafePath(relative)) {
            return null;
        }
        return rootAlias + "." + relative;
    }

    public static boolean isSafePath(String path) {
        return path != null && PATH_PATTERN.matcher(path).matches();
    }

    private static String clause(String path, GridColumnFilter filter) {
        String expression = "LOWER(CAST(" + path + " AS string))";
        String literal = GridLikeEscapes.jpqlLiteral(filter.getValue()).toLowerCase(Locale.ROOT);
        GridFilterOperator operator = filter.getOperator();
        if (operator == GridFilterOperator.EQUALS) {
            return expression + " = '" + literal + "'";
        }
        String pattern = GridLikeEscapes.jpqlLiteral(
                GridLikeEscapes.likePattern(filter.getValue(), operator).toLowerCase(Locale.ROOT));
        String like = expression + " LIKE '" + pattern + "' ESCAPE '\\'";
        if (operator == GridFilterOperator.NOT_CONTAINS) {
            return "(" + path + " IS NULL OR " + like.replaceFirst(" LIKE ", " NOT LIKE ") + ")";
        }
        return like;
    }
}
