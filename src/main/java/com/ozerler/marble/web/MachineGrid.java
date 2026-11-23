package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.MachineDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;

/**
 * Makine listesi. Sort keys match {@code GridPages.MACHINE_SORTS}. Unit filter already exists.
 */
public final class MachineGrid {

    private MachineGrid() {
    }

    public static DataGridView view(TabulatorResponse<MachineDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (MachineDto machine : response.getData()) {
                if (machine != null) {
                    rows.add(row(machine));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "machines-table", 4, columns(), rows);
    }

    public static GridRow row(MachineDto machine) {
        String id = machine.getId() == null ? "" : machine.getId().toString();
        String editUrl = id.isEmpty() ? "" : "/admin/definitions/machines/" + id + "/edit";
        String deleteName = firstText(machine.getName(), machine.getCode());
        return GridRow.of(editUrl, editUrl, machine.getId(), deleteName, List.of(
                GridCell.badge(machine.isActive() ? "Aktif" : "Pasif"),
                GridCell.chip(machine.getId()),
                GridCell.text(machine.getCode()),
                GridCell.text(machine.getName()),
                GridCell.badge(unitLabel(machine.getBusinessUnitLabel(), machine.getBusinessUnit())),
                GridCell.text(machine.getMachineTypeLabel()),
                GridCell.text(machine.getNotes())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("active", "Durum", true, 120),
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("code", "Kod", true, 140),
                GridColumn.text("name", "Makine Adı", true, 180),
                GridColumn.filtered("businessUnitLabel", "İş Birimi", true, 140, "unit", units()),
                GridColumn.text("machineTypeLabel", "Makine Tipi", true, 150),
                GridColumn.text("notes", "Not", false, 160)
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

    private static String unitLabel(String label, String unit) {
        String shown = GridTexts.text(label);
        if (!shown.isBlank()) {
            return shown;
        }
        return GridTexts.text(unit);
    }

    private static String firstText(String preferred, String fallback) {
        String name = GridTexts.text(preferred);
        if (!name.isBlank()) {
            return name;
        }
        return GridTexts.text(fallback);
    }
}
