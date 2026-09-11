package com.ozerler.marble.util;

public final class Csvs {

    private Csvs() {
    }

    public static String escapeField(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
