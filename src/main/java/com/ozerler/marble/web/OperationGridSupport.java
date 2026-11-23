package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Shared display and paging helpers for operation grids. These queries do not accept a sort field.
 */
final class OperationGridSupport {

    private OperationGridSupport() {
    }

    static DataGridView assemble(Page<?> page, int number, int size, String sort, String direction,
                                 String tableId, List<GridColumn> columns, List<GridRow> rows) {
        int totalPages;
        if (page == null) {
            totalPages = 1;
        } else {
            totalPages = page.getTotalPages();
        }
        long total;
        if (page == null) {
            total = 0L;
        } else {
            total = page.getTotalElements();
        }
        return GridAssembler.from(number, size, totalPages, total, sort, direction, tableId,
                GridAssembler.NO_FROZEN_COLUMNS, columns, rows);
    }

    static <T> DataGridView pageOf(Page<T> page, int number, int size, String sort, String direction,
                                   String tableId, List<GridColumn> columns, Function<T, GridRow> mapper) {
        List<GridRow> rows = new ArrayList<>();
        if (page != null && page.getContent() != null) {
            for (T item : page.getContent()) {
                rows.add(mapper.apply(item));
            }
        }
        return assemble(page, number, size, sort, direction, tableId, columns, rows);
    }

    static <E extends Enum<E>> List<GridColumn.FilterChoice> choices(E[] values, Function<E, String> label) {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (E value : values) {
            choices.add(new GridColumn.FilterChoice(value.name(), label.apply(value)));
        }
        return choices;
    }

    static String named(String value, String fallback) {
        if (value == null || value.isBlank()) {
            if (fallback == null) {
                return "";
            }
            return fallback;
        }
        return value;
    }

    static String day(LocalDate value) {
        return GridTexts.date(value);
    }

    static String day(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return GridTexts.date(value.toLocalDate());
    }

    static String amount(BigDecimal value, String unit) {
        String number = GridTexts.quantity(value);
        if (number.isBlank()) {
            return "";
        }
        if (unit == null || unit.isBlank()) {
            return number;
        }
        return number + " " + unit;
    }

    static String centimeters(BigDecimal value) {
        String number = GridTexts.quantity(value);
        if (number.isBlank()) {
            return "2 cm";
        }
        return number + " cm";
    }

    static String rectangle(BigDecimal width, BigDecimal length) {
        if (width == null || length == null) {
            return "";
        }
        return GridTexts.quantity(width) + "×" + GridTexts.quantity(length) + " cm";
    }

    static String pair(String left, String right) {
        boolean hasLeft = left != null && !left.isBlank();
        boolean hasRight = right != null && !right.isBlank();
        if (hasLeft && hasRight) {
            return left + " / " + right;
        }
        if (hasLeft) {
            return left;
        }
        if (hasRight) {
            return right;
        }
        return "";
    }

    static GridCell lines(String text, String subtext) {
        return GridCell.link(text, null, Objects.requireNonNullElse(subtext, ""), null);
    }

    static String customer(String companyName) {
        return named(companyName, "Genel Stok");
    }
}
