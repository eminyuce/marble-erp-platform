package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.StockLocationDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;

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
        return GridAssembler.from(response, page, size, sort, direction, "stock-locations-table",
                GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
    }

    public static GridRow row(StockLocationDto location) {
        String id;
        if (location.getId() == null) {
            id = "";
        } else {
            id = location.getId().toString();
        }
        String editUrl;
        if (id.isEmpty()) {
            editUrl = "";
        } else {
            editUrl = "/admin/definitions/stock-locations/" + id + "/edit";
        }
        String deleteName = named(location.getName(), location.getCode());
        String badge;
        if (location.isActive()) {
            badge = "Aktif";
        } else {
            badge = "Pasif";
        }
        return GridRow.of(editUrl, editUrl, location.getId(), deleteName, List.of(
                GridCell.badge(badge),
                GridCell.chip(location.getId()),
                GridCell.text(location.getCode()),
                GridCell.text(location.getName()),
                GridCell.badge(shownUnit(location.getBusinessUnitLabel(), location.getBusinessUnit())),
                GridCell.text(location.getLocationTypeLabel())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("active", "Durum", true, GridWidth.PX_120),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("code", "Kod", true, GridWidth.PX_150),
                GridColumn.text("name", "Saha / Depo Adı", true, GridWidth.PX_200),
                GridColumn.filtered("businessUnitLabel", "İş Birimi", true, GridWidth.PX_140, "unit", units()),
                GridColumn.text("locationTypeLabel", "Lokasyon Tipi", true, GridWidth.PX_160)
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
        if (text.isBlank()) {
            return GridTexts.text(unit);
        }
        return text;
    }

    private static String named(String name, String code) {
        String text = GridTexts.text(name);
        if (text.isBlank()) {
            return GridTexts.text(code);
        }
        return text;
    }
}
