package com.ozerler.marble.web;

import java.util.List;

/**
 * Value kind for a header-filter column, which decides operators and inputs.
 */
public enum GridFilterKind {

    TEXT,
    DATE,
    NUMBER;

    public GridFilterOperator defaultOperator() {
        if (this == DATE) {
            return GridFilterOperator.BETWEEN;
        }
        if (this == NUMBER) {
            return GridFilterOperator.EQUALS;
        }
        return GridFilterOperator.CONTAINS;
    }

    public List<GridFilterOperator> operators() {
        if (this == DATE) {
            return List.of(
                    GridFilterOperator.BETWEEN,
                    GridFilterOperator.EQUALS,
                    GridFilterOperator.BEFORE,
                    GridFilterOperator.AFTER);
        }
        if (this == NUMBER) {
            return List.of(GridFilterOperator.EQUALS, GridFilterOperator.BETWEEN);
        }
        return List.of(
                GridFilterOperator.CONTAINS,
                GridFilterOperator.EQUALS,
                GridFilterOperator.STARTS_WITH,
                GridFilterOperator.NOT_CONTAINS);
    }

    public boolean supports(GridFilterOperator operator) {
        return operators().contains(operator);
    }

    public String getCode() {
        if (this == DATE) {
            return "date";
        }
        if (this == NUMBER) {
            return "number";
        }
        return "text";
    }

    public static GridFilterKind parse(String raw) {
        if (raw == null) {
            return TEXT;
        }
        if ("date".equalsIgnoreCase(raw) || "datetime".equalsIgnoreCase(raw)) {
            return DATE;
        }
        if ("number".equalsIgnoreCase(raw) || "numeric".equalsIgnoreCase(raw)) {
            return NUMBER;
        }
        return TEXT;
    }
}
