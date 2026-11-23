package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.FactoryWorkOrderSummaryDto;

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
                null, null, "factory-accept-table", GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
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
                GridColumn.text("", "Durum", false, GridWidth.PX_150),
                GridColumn.text("", "Id", false, GridWidth.PX_110),
                GridColumn.text("", "İş Emri No", false, GridWidth.PX_160),
                GridColumn.text("", "Taş cinsi", false, GridWidth.PX_140),
                GridColumn.text("", "Blok", false, GridWidth.PX_140),
                GridColumn.text("", "Makine", false, GridWidth.PX_150),
                GridColumn.text("", "Sorumlu", false, GridWidth.PX_150),
                GridColumn.text("", "Tarih", false, GridWidth.PX_130)
        );
    }
}
