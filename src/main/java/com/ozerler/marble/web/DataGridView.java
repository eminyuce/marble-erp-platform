package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of an enterprise grid: frozen leading columns, body rows, and a stable footer.
 */
public final class DataGridView {

    private final int number;
    private final int size;
    private final int totalPages;
    private final long total;
    private final String sort;
    private final String direction;
    private final String tableId;
    private final int frozenCount;
    private final List<GridColumn> columns;
    private final List<GridRow> rows;
    private final List<PageMarker> markers;

    public DataGridView(int number, int size, int totalPages, long total, String sort, String direction,
                        String tableId, int frozenCount, List<GridColumn> columns, List<GridRow> rows) {
        if (number < 1) {
            this.number = 1;
        } else {
            this.number = number;
        }
        this.size = GridRequests.size(size);
        if (totalPages < 1) {
            this.totalPages = 1;
        } else {
            this.totalPages = totalPages;
        }
        this.total = Math.max(0L, total);
        if (sort == null) {
            this.sort = "";
        } else {
            this.sort = sort;
        }
        if (direction == null) {
            this.direction = "";
        } else {
            this.direction = direction;
        }
        if (tableId == null || tableId.isBlank()) {
            this.tableId = "data-grid";
        } else {
            this.tableId = tableId;
        }
        int columnCount;
        if (columns == null) {
            columnCount = 0;
        } else {
            columnCount = columns.size();
        }
        int requested = Math.max(1, frozenCount);
        this.frozenCount = Math.min(requested, columnCount + 1);
        if (columns == null) {
            this.columns = List.of();
        } else {
            this.columns = List.copyOf(columns);
        }
        if (rows == null) {
            this.rows = List.of();
        } else {
            this.rows = List.copyOf(rows);
        }
        this.markers = markers(this.number, this.totalPages);
    }

    /**
     * Footer tokens: first page, a four-page window around the current page, and the last page.
     * Gaps become ellipses, matching {@code 1 … 11 12 13 14 … 1677}.
     */
    public static List<PageMarker> markers(int current, int total) {
        int safeTotal = Math.max(1, total);
        int safeCurrent = Math.min(Math.max(1, current), safeTotal);
        List<PageMarker> markers = new ArrayList<>();
        if (safeTotal <= 7) {
            for (int page = 1; page <= safeTotal; page++) {
                markers.add(PageMarker.page(page, page == safeCurrent));
            }
            return List.copyOf(markers);
        }
        int start = Math.max(2, safeCurrent - 1);
        int end = Math.min(safeTotal - 1, start + 3);
        start = Math.max(2, end - 3);
        markers.add(PageMarker.page(1, safeCurrent == 1));
        if (start > 2) {
            markers.add(PageMarker.ellipsis());
        }
        for (int page = start; page <= end; page++) {
            markers.add(PageMarker.page(page, page == safeCurrent));
        }
        if (end < safeTotal - 1) {
            markers.add(PageMarker.ellipsis());
        }
        markers.add(PageMarker.page(safeTotal, safeCurrent == safeTotal));
        return List.copyOf(markers);
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotal() {
        return total;
    }

    public String getSort() {
        return sort;
    }

    public String getDirection() {
        return direction;
    }

    public String getTableId() {
        return tableId;
    }

    public int getFrozenCount() {
        return frozenCount;
    }

    public List<GridColumn> getColumns() {
        return columns;
    }

    public List<GridRow> getRows() {
        return rows;
    }

    public List<PageMarker> getMarkers() {
        return markers;
    }

    public boolean isFirst() {
        return number <= 1;
    }

    public boolean isLast() {
        return number >= totalPages;
    }
}
