package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.QuarryDto;
import com.ozerler.marble.dto.TabulatorResponse;

/**
 * Ocak listesi. Sort keys match {@code GridPages.QUARRY_SORTS}.
 */
public final class QuarryGrid {

    private QuarryGrid() {
    }

    public static DataGridView view(TabulatorResponse<QuarryDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (QuarryDto quarry : response.getData()) {
                if (quarry != null) {
                    rows.add(row(quarry));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "quarries-table", 4, columns(), rows);
    }

    public static GridRow row(QuarryDto quarry) {
        String id;
        if (quarry.getId() == null) {
            id = "";
        } else {
            id = quarry.getId().toString();
        }
        String editUrl;
        if (id.isEmpty()) {
            editUrl = "";
        } else {
            editUrl = "/admin/definitions/quarries/" + id + "/edit";
        }
        String deleteName = GridTexts.text(quarry.getName());
        if (deleteName.isBlank()) {
            deleteName = GridTexts.text(quarry.getCode());
        }
        return GridRow.of(editUrl, editUrl, quarry.getId(), deleteName, List.of(
                GridCell.chip(quarry.getId()),
                GridCell.text(quarry.getCode()),
                GridCell.text(quarry.getName()),
                GridCell.text(quarry.getLocation()),
                GridCell.text(GridTexts.quantity(quarry.getSpecificGravity())),
                GridCell.text(quarry.getLicenseNo())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("code", "Ocak Kodu", true, GridWidth.PX_140),
                GridColumn.text("name", "Ocak Adı", true, GridWidth.PX_180),
                GridColumn.text("location", "Lokasyon / Mevkii", true, GridWidth.PX_180),
                GridColumn.text("specificGravity", "Özgül Ağırlık", true, GridWidth.PX_140),
                GridColumn.text("licenseNo", "Ruhsat No", true, GridWidth.PX_140)
        );
    }
}
