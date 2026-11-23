package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.service.ReportService;

/**
 * Selected report preview. Headers come from the report and are not sortable. Rows are paged in memory.
 */
public final class ReportGrid {

    private ReportGrid() {
    }

    public static DataGridView view(ReportService.ReportData report, int page, int size) {
        String[] headers;
        if (report == null || report.getHeaders() == null) {
            headers = new String[0];
        } else {
            headers = report.getHeaders();
        }
        List<String[]> all;
        if (report == null || report.getRows() == null) {
            all = List.of();
        } else {
            all = report.getRows();
        }
        List<GridRow> rows = new ArrayList<>();
        for (String[] cells : GridWindow.page(all, page, size)) {
            rows.add(row(headers, cells));
        }
        return GridAssembler.from(page, size, GridWindow.pageCount(all.size(), size), all.size(),
                null, null, "reports-table", 4, columns(headers), rows);
    }

    private static GridRow row(String[] headers, String[] cells) {
        List<GridCell> values = new ArrayList<>();
        int count = headers.length;
        for (int index = 0; index < count; index++) {
            String value;
            if (cells != null && index < cells.length) {
                value = cells[index];
            } else {
                value = "";
            }
            values.add(GridCell.text(GridWindow.clean(value)));
        }
        return GridRow.of("", "", null, "", values);
    }

    private static List<GridColumn> columns(String[] headers) {
        List<GridColumn> columns = new ArrayList<>();
        for (int index = 0; index < headers.length; index++) {
            String value;
            if (headers[index] == null) {
                value = "";
            } else {
                value = headers[index];
            }
            columns.add(GridColumn.text("", value, false, GridWidth.PX_140));
        }
        return columns;
    }
}
