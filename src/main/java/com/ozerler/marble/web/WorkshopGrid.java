package com.ozerler.marble.web;

import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.OperationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Atölye iş emirleri. Sort keys match {@code GridPages.CUT_ORDER_SORTS}.
 */
public final class WorkshopGrid {

    private WorkshopGrid() {
    }

    public static DataGridView view(TabulatorResponse<CutOrderDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (CutOrderDto order : response.getData()) {
                rows.add(row(order));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "workshop-table", 4, columns(), rows);
    }

    public static GridRow row(CutOrderDto order) {
        String id = order.getId() == null ? "" : order.getId().toString();
        String viewUrl = "/workshop/" + id;
        String editUrl = viewUrl + "/edit";
        String produced = order.getItemCount() + " Adet";
        return GridRow.of(viewUrl, editUrl, null, order.getCutOrderNo(), List.of(
                GridCell.badge(order.getStatusLabel()),
                GridCell.chip(order.getId()),
                GridCell.link(order.getCutOrderNo(), viewUrl),
                GridCell.link(GridWindow.clean(order.getProjectName()), viewUrl),
                GridCell.text(GridWindow.clean(order.getPurposeLabel())),
                GridCell.text(GridWindow.clean(order.getLocationName())),
                GridCell.text(GridWindow.clean(order.getMachineName())),
                GridCell.text(GridWindow.clean(order.getOperatorName())),
                GridCell.link(produced, null, GridWindow.withUnit(order.getTotalAreaM2(), "m²"), null)
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", true, 150, "status", statuses()),
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("cutOrderNo", "İş Emri No", true, 170),
                GridColumn.text("projectName", "Şantiye / Müşteri", true, 180),
                GridColumn.text("", "Amaç", false, 150),
                GridColumn.text("locationName", "Hedef Mahal", true, 150),
                GridColumn.text("machineName", "Makine", true, 150),
                GridColumn.text("operatorName", "Operatör", true, 140),
                GridColumn.text("", "Üretilen Mamul", false, 160)
        );
    }

    private static List<GridColumn.FilterChoice> statuses() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (OperationStatus status : OperationStatus.values()) {
            choices.add(new GridColumn.FilterChoice(status.name(), status.getLabel()));
        }
        return choices;
    }
}
