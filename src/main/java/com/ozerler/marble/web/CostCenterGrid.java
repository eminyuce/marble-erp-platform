package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.CostCenterDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;

/**
 * Masraf merkezi listesi. Sort keys match {@code GridPages.COST_CENTER_SORTS}. Unit filter already exists.
 */
public final class CostCenterGrid {

    private CostCenterGrid() {
    }

    public static DataGridView view(TabulatorResponse<CostCenterDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (CostCenterDto center : response.getData()) {
                if (center != null) {
                    rows.add(row(center));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "cost-centers-table", 4, columns(), rows);
    }

    public static GridRow row(CostCenterDto center) {
        String id = center.getId() == null ? "" : center.getId().toString();
        String editUrl = id.isEmpty() ? "" : "/admin/definitions/cost-centers/" + id + "/edit";
        String deleteName = GridTexts.text(center.getName());
        if (deleteName.isBlank()) {
            deleteName = GridTexts.text(center.getCode());
        }
        return GridRow.of(editUrl, editUrl, center.getId(), deleteName, List.of(
                GridCell.chip(center.getId()),
                GridCell.text(center.getCode()),
                GridCell.text(center.getName()),
                GridCell.badge(labelOrCode(center.getBusinessUnitLabel(), center.getBusinessUnit())),
                GridCell.text(GridTexts.money(center.getMonthlyBudget())),
                GridCell.text(center.getDescription())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("code", "Merkez Kodu", true, 140),
                GridColumn.text("name", "Masraf Merkezi Adı", true, 200),
                GridColumn.filtered("businessUnitLabel", "İş Birimi", true, 140, "unit", units()),
                GridColumn.text("monthlyBudget", "Aylık Bütçe", true, 150),
                GridColumn.text("description", "Açıklama", false, 180)
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

    private static String labelOrCode(String label, String code) {
        String text = GridTexts.text(label);
        return text.isBlank() ? GridTexts.text(code) : text;
    }
}
