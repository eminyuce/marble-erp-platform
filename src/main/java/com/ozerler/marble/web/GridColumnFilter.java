package com.ozerler.marble.web;

/**
 * One active header filter: a whitelisted column key, an operator, and the user value(s).
 */
public final class GridColumnFilter {

    private final String key;
    private final GridFilterOperator operator;
    private final String value;
    private final String toValue;

    public GridColumnFilter(String key, GridFilterOperator operator, String value) {
        this(key, operator, value, "");
    }

    public GridColumnFilter(String key, GridFilterOperator operator, String value, String toValue) {
        if (key == null) {
            this.key = "";
        } else {
            this.key = key;
        }
        if (operator == null) {
            this.operator = GridFilterOperator.CONTAINS;
        } else {
            this.operator = operator;
        }
        if (value == null) {
            this.value = "";
        } else {
            this.value = value;
        }
        if (toValue == null) {
            this.toValue = "";
        } else {
            this.toValue = toValue;
        }
    }

    public String getKey() {
        return key;
    }

    public GridFilterOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public String getToValue() {
        return toValue;
    }

    public boolean isActive() {
        if (key.isBlank()) {
            return false;
        }
        if (operator == GridFilterOperator.BETWEEN) {
            return !value.isBlank() && !toValue.isBlank();
        }
        return !value.isBlank();
    }
}
