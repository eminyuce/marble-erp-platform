package com.ozerler.marble.web;

import java.util.Locale;

/**
 * Operators for the shared per-column header filter.
 */
public enum GridFilterOperator {

    CONTAINS("contains", "İçerir"),
    EQUALS("equals", "Eşittir"),
    STARTS_WITH("starts", "İle Başlar"),
    NOT_CONTAINS("not_contains", "İçermez"),
    BETWEEN("between", "Aralık"),
    BEFORE("before", "Önce"),
    AFTER("after", "Sonra");

    private final String code;
    private final String label;

    GridFilterOperator(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static GridFilterOperator parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return CONTAINS;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (GridFilterOperator operator : values()) {
            if (operator.code.equals(value)) {
                return operator;
            }
        }
        if ("startswith".equals(value) || "starts_with".equals(value) || "ile_baslar".equals(value)) {
            return STARTS_WITH;
        }
        if ("notcontains".equals(value) || "does_not_contain".equals(value) || "icermez".equals(value)) {
            return NOT_CONTAINS;
        }
        if ("eq".equals(value) || "equal".equals(value) || "esittir".equals(value)) {
            return EQUALS;
        }
        if ("icerir".equals(value) || "like".equals(value)) {
            return CONTAINS;
        }
        if ("between".equals(value) || "range".equals(value)
                || "aralik".equals(value) || "aralık".equals(value)) {
            return BETWEEN;
        }
        if ("before".equals(value) || "lt".equals(value)
                || "once".equals(value) || "önce".equals(value)) {
            return BEFORE;
        }
        if ("after".equals(value) || "gt".equals(value)
                || "sonra".equals(value)) {
            return AFTER;
        }
        return CONTAINS;
    }

    public static GridFilterOperator parse(String raw, GridFilterKind kind) {
        if (raw == null || raw.isBlank()) {
            if (kind == null) {
                return CONTAINS;
            }
            return kind.defaultOperator();
        }
        GridFilterOperator operator = parse(raw);
        if (kind != null && !kind.supports(operator)) {
            return kind.defaultOperator();
        }
        return operator;
    }
}
