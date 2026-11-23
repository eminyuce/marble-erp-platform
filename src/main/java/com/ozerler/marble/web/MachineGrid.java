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
        String id;
        if (machine.getId() == null) {
            id = "";
        } else {
            id = machine.getId().toString();
        }
        String editUrl;
        if (id.isEmpty()) {
            editUrl = "";
        } else {
            editUrl = "/admin/definitions/machines/" + id + "/edit";
        }
        String deleteName = firstText(machine.getName(), machine.getCode());
        String badge;
        if (machine.isActive()) {
            badge = "Aktif";
        } else {
            badge = "Pasif";
        }
        return GridRow.of(editUrl, editUrl, machine.getId(), deleteName, List.of(
                GridCell.badge(badge),
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
                GridColumn.text("active", "Durum", true, GridWidth.PX_120),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("code", "Kod", true, GridWidth.PX_140),
                GridColumn.text("name", "Makine Adı", true, GridWidth.PX_180),
                GridColumn.filtered("businessUnitLabel", "İş Birimi", true, GridWidth.PX_140, "unit", units()),
                GridColumn.text("machineTypeLabel", "Makine Tipi", true, GridWidth.PX_150),
                GridColumn.text("notes", "Not", false, GridWidth.PX_160)
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
