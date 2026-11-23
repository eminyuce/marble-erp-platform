package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private GridColumnFilterJpql() {
    }

    public static String predicate(String rootAlias, Map<String, String> aliases, GridColumnFilters filters) {
        return predicate(rootAlias, aliases, filters, GridFilterCatalog.entityTypeForAliases(aliases));
    }

    public static String predicate(String rootAlias, Map<String, String> aliases, GridColumnFilters filters,
                                   Class<?> entityType) {
        if (filters == null || filters.isEmpty() || !isSafePath(rootAlias)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (GridColumnFilter filter : filters.getItems()) {
            String path = propertyPath(rootAlias, aliases, filter.getKey());
            if (path == null) {
                continue;
            }
            Class<?> javaType = GridFilterTypes.javaType(entityType, aliases.get(filter.getKey()));
            String clause = clause(path, javaType, filter);
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

    private static String clause(String path, Class<?> javaType, GridColumnFilter filter) {
        GridFilterKind kind = GridFilterTypes.kindOf(javaType);
        if (kind == GridFilterKind.DATE) {
            return dateClause(path, javaType, filter);
        }
        if (kind == GridFilterKind.NUMBER) {
            return numberClause(path, filter);
        }
        return textClause(path, filter);
    }

    private static String textClause(String path, GridColumnFilter filter) {
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

    private static String dateClause(String path, Class<?> javaType, GridColumnFilter filter) {
        LocalDate from = GridFilterTypes.parseDate(filter.getValue());
        if (from == null) {
            return "";
        }
        GridFilterOperator operator = filter.getOperator();
        if (LocalDate.class.equals(javaType)) {
            if (operator == GridFilterOperator.BEFORE) {
                return path + " < " + d(from);
            }
            if (operator == GridFilterOperator.AFTER) {
                return path + " > " + d(from);
            }
            if (operator == GridFilterOperator.BETWEEN) {
                LocalDate to = GridFilterTypes.parseDate(filter.getToValue());
                if (to == null) {
                    return "";
                }
                return path + " >= " + d(from) + " AND " + path + " <= " + d(to);
            }
            return path + " = " + d(from);
        }
        String start = ts(startLiteral(javaType, from));
        String next = ts(startLiteral(javaType, from.plusDays(1)));
        if (operator == GridFilterOperator.BEFORE) {
            return path + " < " + start;
        }
        if (operator == GridFilterOperator.AFTER) {
            return path + " >= " + next;
        }
        if (operator == GridFilterOperator.BETWEEN) {
            LocalDate to = GridFilterTypes.parseDate(filter.getToValue());
            if (to == null) {
                return "";
            }
            return path + " >= " + start + " AND " + path + " < " + ts(startLiteral(javaType, to.plusDays(1)));
        }
        return path + " >= " + start + " AND " + path + " < " + next;
    }

    private static String numberClause(String path, GridColumnFilter filter) {
        BigDecimal from = GridFilterTypes.parseNumber(filter.getValue());
        if (from == null) {
            return "";
        }
        if (filter.getOperator() == GridFilterOperator.BETWEEN) {
            BigDecimal to = GridFilterTypes.parseNumber(filter.getToValue());
            if (to == null) {
                return "";
            }
            return path + " >= " + from.toPlainString() + " AND " + path + " <= " + to.toPlainString();
        }
        return path + " = " + from.toPlainString();
    }

    private static String d(LocalDate date) {
        return "cast('" + date + "' as date)";
    }

    private static String ts(LocalDateTime dateTime) {
        return "cast('" + dateTime.format(TIMESTAMP) + "' as timestamp)";
    }

    private static LocalDateTime startLiteral(Class<?> javaType, LocalDate date) {
        if (java.time.Instant.class.equals(javaType)
                || java.time.OffsetDateTime.class.equals(javaType)
                || java.time.ZonedDateTime.class.equals(javaType)
                || java.util.Date.class.equals(javaType)) {
            return LocalDateTime.ofInstant(GridFilterTypes.startOfDayInstant(date), java.time.ZoneOffset.UTC);
        }
        return GridFilterTypes.startOfDay(date);
    }
}
