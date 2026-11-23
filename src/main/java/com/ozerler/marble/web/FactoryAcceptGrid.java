package com.ozerler.marble.web;

import com.ozerler.marble.dto.FactoryWorkOrderSummaryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabrika blok kabul listesi. Rows are already loaded and paged in memory. Columns are not sortable.
 */
public final class FactoryAcceptGrid {

    private FactoryAcceptGrid() {
    }

    public static DataGridView view(List<FactoryWorkOrderSummaryDto> orders, int page, int size, String search) {
        List<FactoryWorkOrderSummaryDto> matched = new ArrayList<>();
        if (orders != null) {
            for (FactoryWorkOrderSummaryDto order : orders) {
                if (GridWindow.matches(search, order.getOrderNo(), order.getBlockCode(), order.getStoneType(),
                        order.getMachineName(), order.getResponsibleName(), order.getStatusLabel())) {
                    matched.add(order);
                }
            }
        }
        List<GridRow> rows = new ArrayList<>();
        for (FactoryWorkOrderSummaryDto order : GridWindow.page(matched, page, size)) {
            rows.add(row(order));
        }
        return GridAssembler.from(page, size, GridWindow.pageCount(matched.size(), size), matched.size(),
                null, null, "factory-accept-table", 4, columns(), rows);
    }

    private static GridRow row(FactoryWorkOrderSummaryDto order) {
        return GridRow.of("", "", null, order.getOrderNo(), List.of(
                GridCell.badge(order.getStatusLabel()),
                GridCell.chip(order.getId()),
                GridCell.text(GridWindow.clean(order.getOrderNo())),
                GridCell.text(GridWindow.clean(order.getStoneType())),
                GridCell.text(GridWindow.clean(order.getBlockCode())),
                GridCell.text(GridWindow.clean(order.getMachineName())),
                GridCell.text(GridWindow.clean(order.getResponsibleName())),
                GridCell.text(GridWindow.isoDate(order.getAcceptedAt()))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("", "Durum", false, 150),
                GridColumn.text("", "Id", false, 110),
                GridColumn.text("", "İş Emri No", false, 160),
                GridColumn.text("", "Taş cinsi", false, 140),
                GridColumn.text("", "Blok", false, 140),
                GridColumn.text("", "Makine", false, 150),
                GridColumn.text("", "Sorumlu", false, 150),
                GridColumn.text("", "Tarih", false, 130)
        );
    }
}
