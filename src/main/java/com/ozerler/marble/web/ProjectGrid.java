package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.ProjectStatus;

/**
 * Şantiye listesi columns. Sort keys match {@code GridPages.PROJECT_SORTS}. Status filter already exists.
 */
public final class ProjectGrid {

    private ProjectGrid() {
    }

    public static DataGridView view(TabulatorResponse<ProjectDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (ProjectDto project : response.getData()) {
                rows.add(row(project));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "projects-table",
                GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
    }

    public static GridRow row(ProjectDto project) {
        String id;
        if (project.getId() == null) {
            id = "";
        } else {
            id = project.getId().toString();
        }
        String viewUrl = "/projects/" + id;
        String editUrl = viewUrl + "/edit";
        String customerHref;
        if (project.getCustomerId() == null) {
            customerHref = null;
        } else {
            customerHref = "/admin/definitions/customers/" + project.getCustomerId() + "/transactions";
        }
        String installed = GridTexts.quantity(project.getTotalInstalledAreaM2());
        String planned = GridTexts.quantity(project.getTotalPlannedAreaM2());
        String progress;
        if (installed.isBlank() && planned.isBlank()) {
            progress = "";
        } else {
            progress = installed + " / " + planned + " m²";
        }
        return GridRow.of(viewUrl, editUrl, project.getId(), project.getName(), List.of(
                GridCell.badge(project.getStatusLabel()),
                GridCell.chip(project.getId()),
                GridCell.link(project.getProjectCode(), viewUrl),
                GridCell.link(project.getName(), viewUrl, project.getCustomerName(), customerHref),
                GridCell.text(GridTexts.money(project.getContractValue())),
                GridCell.text(GridTexts.money(project.getActualCost())),
                GridCell.text(progress)
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", true, GridWidth.PX_180, "status", statuses()),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("projectCode", "Şantiye Kodu", true, GridWidth.PX_160),
                GridColumn.text("name", "Şantiye Adı", true, GridWidth.PX_240),
                GridColumn.text("customerName", "Müşteri", true, GridWidth.PX_200),
                GridColumn.text("contractValue", "Sözleşme Bedeli", true, GridWidth.PX_170),
                GridColumn.text("actualCost", "Gerçekleşen Maliyet", true, GridWidth.PX_180),
                GridColumn.text("", "Montaj İlerlemesi", false, GridWidth.PX_160)
        );
    }

    private static List<GridColumn.FilterChoice> statuses() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (ProjectStatus status : ProjectStatus.values()) {
            choices.add(new GridColumn.FilterChoice(status.name(), status.getLabel()));
        }
        return choices;
    }
}
