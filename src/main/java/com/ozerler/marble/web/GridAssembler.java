package com.ozerler.marble.web;

import java.util.List;

import com.ozerler.marble.dto.TabulatorResponse;

/**
 * Builds a {@link DataGridView} from an already paged result.
 */
public final class GridAssembler {

    public static final int NO_FROZEN_COLUMNS = DataGridView.NO_FROZEN_COLUMNS;

    private GridAssembler() {
    }

    public static DataGridView from(TabulatorResponse<?> response, int page, int size, String sort, String direction,
                                    String tableId, int frozenCount, List<GridColumn> columns, List<GridRow> rows) {
        int totalPages;
        if (response == null) {
            totalPages = 1;
        } else {
            totalPages = response.getLastPage();
        }
        long total;
        if (response == null) {
            total = 0L;
        } else {
            total = response.getTotal();
        }
        return new DataGridView(page, size, totalPages, total, sort, direction, tableId, frozenCount, columns, rows);
    }

    public static DataGridView from(int page, int size, int totalPages, long total, String sort, String direction,
                                    String tableId, int frozenCount, List<GridColumn> columns, List<GridRow> rows) {
        return new DataGridView(page, size, totalPages, total, sort, direction, tableId, frozenCount, columns, rows);
    }
}
