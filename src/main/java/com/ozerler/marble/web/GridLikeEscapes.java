package com.ozerler.marble.web;

/**
 * Escapes user text for SQL {@code LIKE} patterns and JPQL string literals.
 */
public final class GridLikeEscapes {

    public static final char ESCAPE_CHAR = '\\';

    private GridLikeEscapes() {
    }

    public static String escape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(raw.length() + 4);
        for (int index = 0; index < raw.length(); index++) {
            char character = raw.charAt(index);
            if (character == ESCAPE_CHAR || character == '%' || character == '_') {
                escaped.append(ESCAPE_CHAR);
            }
            escaped.append(character);
        }
        return escaped.toString();
    }

    public static String likePattern(String raw, GridFilterOperator operator) {
        String escaped = escape(raw);
        if (operator == GridFilterOperator.STARTS_WITH) {
            return escaped + "%";
        }
        if (operator == GridFilterOperator.CONTAINS || operator == GridFilterOperator.NOT_CONTAINS) {
            return "%" + escaped + "%";
        }
        return escaped;
    }

    public static String jpqlLiteral(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("'", "''");
    }
}
