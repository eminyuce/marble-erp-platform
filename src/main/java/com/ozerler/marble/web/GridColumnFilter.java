package com.ozerler.marble.web;

/**
 * One active header filter: a whitelisted column key, an operator, and the user value.
 */
public final class GridColumnFilter {

    private final String key;
    private final GridFilterOperator operator;
    private final String value;

    public GridColumnFilter(String key, GridFilterOperator operator, String value) {
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

    public boolean isActive() {
        return !key.isBlank() && !value.isBlank();
    }
}
