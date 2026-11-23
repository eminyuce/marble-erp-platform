package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.ProductionOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.OperationStatus;

/**
 * Fabrika kesim iş emirleri. Sort keys match {@code GridPages.PRODUCTION_ORDER_SORTS}.
 */
public final class ProductionOrderGrid {

    private ProductionOrderGrid() {
    }

    public static DataGridView view(TabulatorResponse<ProductionOrderDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (ProductionOrderDto order : response.getData()) {
                rows.add(row(order));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "production-table",
                GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
    }

    public static GridRow row(ProductionOrderDto order) {
        String id;
        if (order.getId() == null) {
            id = "";
        } else {
            id = order.getId().toString();
        }
        String viewUrl = "/production/orders/" + id;
        String editUrl = viewUrl + "/edit";
        String hours = GridTexts.quantity(order.getDurationHours());
        String energy = GridTexts.quantity(order.getElectricityKwh());
        String duration;
        if (hours.isBlank()) {
            duration = "";
        } else {
            duration = hours + " sa";
        }
        String consumption;
        if (energy.isBlank()) {
            consumption = "";
        } else {
            consumption = energy + " kWh";
        }
        String slabs = order.getSlabCount() + " Plaka";
        return GridRow.of(
                viewUrl,
                editUrl,
                null,
                order.getOrderNo(),
                List.of(
                        GridCell.badge(order.getStatusLabel()),
                        GridCell.chip(order.getId()),
                        GridCell.link(order.getOrderNo(), viewUrl),
                        GridCell.link(GridWindow.clean(order.getMachineName()), viewUrl),
                        GridCell.link(
                                GridWindow.clean(order.getBlockCode()),
                                null,
                                GridWindow.clean(order.getStoneType()),
                                null),
                        GridCell.link(duration, null, consumption, null),
                        GridCell.link(
                                slabs,
                                null,
                                GridWindow.withUnit(order.getTotalSlabAreaM2(), "m²"),
                                null),
                        GridCell.text(GridWindow.clean(order.getOperatorName()))));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", true, GridWidth.PX_150, "status", statuses()),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("orderNo", "İş Emri No", true, GridWidth.PX_170),
                GridColumn.text("machineName", "Makine / Hat", true, GridWidth.PX_170),
                GridColumn.text("", "Kaynak Blok", false, GridWidth.PX_160),
                GridColumn.text("", "Süre / Tüketim", false, GridWidth.PX_160),
                GridColumn.text("", "Çıkan Plakalar", false, GridWidth.PX_150),
                GridColumn.text("operatorName", "Operatör", true, GridWidth.PX_150)
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
