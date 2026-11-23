package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.SlabDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.SlabStatus;

/**
 * Plaka stok listesi. Sort keys match {@code GridPages.SLAB_SORTS}.
 */
public final class SlabGrid {

    private SlabGrid() {
    }

    public static DataGridView view(TabulatorResponse<SlabDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (SlabDto slab : response.getData()) {
                rows.add(row(slab));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "slabs-table", 4, columns(), rows);
    }

    public static GridRow row(SlabDto slab) {
        String id;
        if (slab.getId() == null) {
            id = "";
        } else {
            id = slab.getId().toString();
        }
        String viewUrl = "/production/slabs/" + id;
        String editUrl = viewUrl + "/edit";
        String code = GridWindow.clean(slab.getSlabCode());
        String passport;
        if (code.isBlank()) {
            passport = "";
        } else {
            passport = "/passport/" + code;
        }
        return GridRow.of(viewUrl, editUrl, null, code, List.of(
                GridCell.badge(slab.getStatusLabel()),
                GridCell.chip(slab.getId()),
                GridCell.link(code, viewUrl),
                GridCell.text(dimensions(slab)),
                GridCell.text(GridWindow.withUnit(slab.getSurfaceAreaM2(), "m²")),
                GridCell.text(label(slab.getSurfaceFinishLabel(), slab.getSurfaceFinish())),
                GridCell.text(label(slab.getQualityGradeLabel(), slab.getQualityGrade())),
                GridCell.text(GridTexts.money(slab.getCostPerM2())),
                GridCell.link("Pasaport", passport),
                GridCell.link("Barkod", viewUrl + "/label")
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", true, GridWidth.PX_150, "status", statuses()),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("slabCode", "Plaka Kodu", true, GridWidth.PX_160),
                GridColumn.text("", "Ebatlar (cm)", false, GridWidth.PX_180),
                GridColumn.text("surfaceAreaM2", "Net Alan", true, GridWidth.PX_120),
                GridColumn.text("surfaceFinish", "Yüzey", true, GridWidth.PX_150),
                GridColumn.text("qualityGrade", "Kalite", true, GridWidth.PX_120),
                GridColumn.text("costPerM2", "Birim Maliyet (m²)", true, GridWidth.PX_160),
                GridColumn.text("", "Pasaport", false, GridWidth.PX_120),
                GridColumn.text("", "Barkod", false, GridWidth.PX_120)
        );
    }

    private static List<GridColumn.FilterChoice> statuses() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (SlabStatus status : SlabStatus.values()) {
            choices.add(new GridColumn.FilterChoice(status.name(), status.getLabel()));
        }
        return choices;
    }

    private static String dimensions(SlabDto slab) {
        String thickness = GridTexts.quantity(slab.getThicknessCm());
        String width = GridTexts.quantity(slab.getWidthCm());
        String length = GridTexts.quantity(slab.getLengthCm());
        if (thickness.isBlank() && width.isBlank() && length.isBlank()) {
            return "";
        }
        return thickness + " cm • " + width + "x" + length + " cm";
    }

    private static String label(String preferred, String fallback) {
        String text = GridWindow.clean(preferred);
        if (text.isBlank()) {
            return GridWindow.clean(fallback);
        }
        return text;
    }
}
