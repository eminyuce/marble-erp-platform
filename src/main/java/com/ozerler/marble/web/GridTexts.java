package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display text for grid cells. Blank results render as a dash in the fragment.
 */
public final class GridTexts {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private GridTexts() {
    }

    public static String text(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("tr-TR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value) + " ₺";
    }

    public static String date(LocalDate value) {
        if (value == null) {
            return "";
        }
        return DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return DATE_TIME.format(value);
    }

    public static String quantity(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
