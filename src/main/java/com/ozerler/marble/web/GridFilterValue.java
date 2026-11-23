package com.ozerler.marble.web;

/**
 * One distinct value shown in the header-filter list.
 */
public final class GridFilterValue {

    private final String value;
    private final String label;

    public GridFilterValue(String value, String label) {
        if (value == null) {
            this.value = "";
        } else {
            this.value = value;
        }
        if (label == null || label.isBlank()) {
            this.label = this.value;
        } else {
            this.label = label;
        }
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
