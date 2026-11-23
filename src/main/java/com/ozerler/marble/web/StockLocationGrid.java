package com.ozerler.marble.web;

import com.ozerler.marble.dto.StockLocationDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * Stok sahası listesi. Sort keys match {@code GridPages.STOCK_LOCATION_SORTS}. Unit filter already exists.
 */
public final class StockLocationGrid {

    private StockLocationGrid() {
    }

    public static DataGridView view(TabulatorResponse<StockLocationDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (StockLocationDto location : response.getData()) {
                if (location != null) {
                    rows.add(row(location));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "stock-locations-table", 4, columns(), rows);
    }

    public static GridRow row(StockLocationDto location) {
        String id = location.getId() == null ? "" : location.getId().toString();
        String editUrl = id.isEmpty() ? "" : "/admin/definitions/stock-locations/" + id + "/edit";
        String deleteName = named(location.getName(), location.getCode());
        return GridRow.of(editUrl, editUrl, location.getId(), deleteName, List.of(
                GridCell.badge(location.isActive() ? "Aktif" : "Pasif"),
                GridCell.chip(location.getId()),
                GridCell.text(location.getCode()),
                GridCell.text(location.getName()),
                GridCell.badge(shownUnit(location.getBusinessUnitLabel(), location.getBusinessUnit())),
                GridCell.text(location.getLocationTypeLabel())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("active", "Durum", true, 120),
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("code", "Kod", true, 150),
                GridColumn.text("name", "Saha / Depo Adı", true, 200),
                GridColumn.filtered("businessUnitLabel", "İş Birimi", true, 140, "unit", units()),
                GridColumn.text("locationTypeLabel", "Lokasyon Tipi", true, 160)
        );
    }

    private static List<GridColumn.FilterChoice> units() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (BusinessUnit unit : BusinessUnit.values()) {
            choices.add(new GridColumn.FilterChoice(unit.name(), unit.getLabel()));
        }
        return choices;
    }

    private static String shownUnit(String label, String unit) {
        String text = GridTexts.text(label);
        return text.isBlank() ? GridTexts.text(unit) : text;
    }

    private static String named(String name, String code) {
        String text = GridTexts.text(name);
        return text.isBlank() ? GridTexts.text(code) : text;
    }
}
