package com.ozerler.marble.web;

import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.ProjectStatus;

import java.util.ArrayList;
import java.util.List;

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
        return GridAssembler.from(response, page, size, sort, direction, "projects-table", 4, columns(), rows);
    }

    public static GridRow row(ProjectDto project) {
        String id = project.getId() == null ? "" : project.getId().toString();
        String viewUrl = "/projects/" + id;
        String editUrl = viewUrl + "/edit";
        String customerHref = project.getCustomerId() == null
                ? null
                : "/admin/definitions/customers/" + project.getCustomerId() + "/transactions";
        String installed = GridTexts.quantity(project.getTotalInstalledAreaM2());
        String planned = GridTexts.quantity(project.getTotalPlannedAreaM2());
        String progress = installed.isBlank() && planned.isBlank() ? "" : installed + " / " + planned + " m²";
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
                GridColumn.filtered("status", "Durum", true, 180, "status", statuses()),
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("projectCode", "Şantiye Kodu", true, 160),
                GridColumn.text("name", "Şantiye Adı", true, 240),
                GridColumn.text("customerName", "Müşteri", true, 200),
                GridColumn.text("contractValue", "Sözleşme Bedeli", true, 170),
                GridColumn.text("actualCost", "Gerçekleşen Maliyet", true, 180),
                GridColumn.text("", "Montaj İlerlemesi", false, 160)
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
