package com.ozerler.marble.domain;

import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.util.MessageUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Costing period stored as YYYY-MM. Invoice month and costing month may differ.
 */
public final class ExpensePeriods {

    public static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private ExpensePeriods() {
    }

    public static String normalize(String period) {
        Objects.requireNonNull(period, MessageUtils.getMessage("error.expense.period.required"));
        try {
            return YearMonth.parse(period.trim(), YEAR_MONTH).toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.expense.period.invalid", period), ex);
        }
    }

    public static String of(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth, MessageUtils.getMessage("error.expense.period.required"));
        return yearMonth.toString();
    }

    public static YearMonth parse(String period) {
        return YearMonth.parse(normalize(period), YEAR_MONTH);
    }

    public static String postingPeriod(LocalDate entryDate) {
        LocalDate reference = entryDate != null ? entryDate : LocalDate.now();
        return of(YearMonth.from(reference));
    }

    /**
     * Electricity invoices are posted in the arrival month and costed to the previous production month
     * unless the caller supplies an explicit consumption period.
     */
    public static String expensePeriod(ExpenseType type, LocalDate invoiceDate, LocalDate entryDate) {
        LocalDate reference = invoiceDate != null ? invoiceDate : (entryDate != null ? entryDate : LocalDate.now());
        YearMonth month = YearMonth.from(reference);
        if (type == ExpenseType.ELECTRICITY) {
            return of(month.minusMonths(1));
        }
        return of(month);
    }
}
