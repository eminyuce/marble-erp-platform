package com.ozerler.marble.web;

import com.ozerler.marble.dto.view.SlabView;
import com.ozerler.marble.model.enums.SlabStatus;

/**
 * Cells and headers shared by the factory and workshop slab yards.
 */
final class SlabGridCells {

    private SlabGridCells() {
    }

    static GridCell status(SlabView slab) {
        String badge;
        if (slab.getStatus() == null) {
            badge = "Hazır";
        } else {
            badge = slab.getStatus().getDisplayName();
        }
        return GridCell.badge(badge);
    }

    static GridCell code(SlabView slab) {
        return GridCell.text(slab.getSlabCode());
    }

    static GridCell created(SlabView slab) {
        return GridCell.text(OperationGridSupport.day(slab.getCreatedDate()));
    }

    static GridCell block(SlabView slab) {
        String value;
        if (slab.getBlock() == null) {
            value = "";
        } else {
            value = slab.getBlock().getBlockCode();
        }
        return GridCell.text(value);
    }

    static GridCell stone(SlabView slab) {
        String value;
        if (slab.getBlock() == null) {
            value = "";
        } else {
            value = slab.getBlock().getStoneType();
        }
        return GridCell.text(value);
    }

    static GridCell finish(SlabView slab) {
        return GridCell.text(OperationGridSupport.pair(quality(slab), surface(slab)));
    }

    static GridCell thickness(SlabView slab) {
        return GridCell.text(OperationGridSupport.centimeters(slab.getThicknessCm()));
    }

    static GridCell dimensions(SlabView slab) {
        return GridCell.text(OperationGridSupport.rectangle(slab.getWidthCm(), slab.getLengthCm()));
    }

    static GridCell area(SlabView slab) {
        return GridCell.text(OperationGridSupport.amount(slab.getSurfaceAreaM2(), "m²"));
    }

    static GridCell customer(SlabView slab) {
        String name;
        if (slab.getCustomer() == null) {
            name = null;
        } else {
            name = slab.getCustomer().getCompanyName();
        }
        return GridCell.text(OperationGridSupport.customer(name));
    }

    static GridColumn statusColumn() {
        return GridColumn.filtered("status", "Durum", false, 140, "status",
                OperationGridSupport.choices(SlabStatus.values(), SlabStatus::getDisplayName));
    }

    static GridColumn idColumn() {
        return GridColumn.text("id", "Id", false, 110);
    }

    static GridColumn codeColumn() {
        return GridColumn.text("slabCode", "Plaka No", false, 140);
    }

    static GridColumn blockColumn() {
        return GridColumn.text("sourceBlockCode", "Kaynak Blok", false, 140);
    }

    static GridColumn stoneColumn() {
        return GridColumn.text("stoneType", "Taş Türü", false, 140);
    }

    static GridColumn finishColumn() {
        return GridColumn.text("qualityGrade", "Kalite / Yüzey", false, 160);
    }

    static GridColumn thicknessColumn() {
        return GridColumn.text("thicknessCm", "Kalınlık", false, 110);
    }

    static GridColumn dimensionsColumn() {
        return GridColumn.text("dimensions", "Ölçü (En×Boy)", false, 150);
    }

    static GridColumn areaColumn() {
        return GridColumn.text("surfaceAreaM2", "M²", false, 120);
    }

    private static String quality(SlabView slab) {
        if (slab.getQualityGrade() == null) {
            return "1. Sınıf";
        }
        return slab.getQualityGrade().getDisplayName();
    }

    private static String surface(SlabView slab) {
        if (slab.getSurfaceFinish() == null) {
            return "Cilalı";
        }
        return slab.getSurfaceFinish().getDisplayName();
    }
}
