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
        String[] headers = report == null || report.getHeaders() == null ? new String[0] : report.getHeaders();
        List<String[]> all = report == null || report.getRows() == null ? List.of() : report.getRows();
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
            String value = cells != null && index < cells.length ? cells[index] : "";
            values.add(GridCell.text(GridWindow.clean(value)));
        }
        return GridRow.of("", "", null, "", values);
    }

    private static List<GridColumn> columns(String[] headers) {
        List<GridColumn> columns = new ArrayList<>();
        for (int index = 0; index < headers.length; index++) {
            columns.add(GridColumn.text("", headers[index] == null ? "" : headers[index], false, 140));
        }
        return columns;
    }
}
