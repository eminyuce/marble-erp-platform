package com.ozerler.marble.web;

import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.SlabStatus;

/**
 * Cells and headers shared by the factory and workshop slab yards.
 */
final class SlabGridCells {

    private SlabGridCells() {
    }

    static GridCell status(Slab slab) {
        return GridCell.badge(slab.getStatus() == null ? "Hazır" : slab.getStatus().getDisplayName());
    }

    static GridCell code(Slab slab) {
        return GridCell.text(slab.getSlabCode());
    }

    static GridCell created(Slab slab) {
        return GridCell.text(OperationGridSupport.day(slab.getCreatedDate()));
    }

    static GridCell block(Slab slab) {
        return GridCell.text(slab.getBlock() == null ? "" : slab.getBlock().getBlockCode());
    }

    static GridCell stone(Slab slab) {
        return GridCell.text(slab.getBlock() == null ? "" : slab.getBlock().getStoneType());
    }

    static GridCell finish(Slab slab) {
        return GridCell.text(OperationGridSupport.pair(quality(slab), surface(slab)));
    }

    static GridCell thickness(Slab slab) {
        return GridCell.text(OperationGridSupport.centimeters(slab.getThicknessCm()));
    }

    static GridCell dimensions(Slab slab) {
        return GridCell.text(OperationGridSupport.rectangle(slab.getWidthCm(), slab.getLengthCm()));
    }

    static GridCell area(Slab slab) {
        return GridCell.text(OperationGridSupport.amount(slab.getSurfaceAreaM2(), "m²"));
    }

    static GridCell customer(Slab slab) {
        String name = slab.getCustomer() == null ? null : slab.getCustomer().getCompanyName();
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

    private static String quality(Slab slab) {
        return slab.getQualityGrade() == null ? "1. Sınıf" : slab.getQualityGrade().getDisplayName();
    }

    private static String surface(Slab slab) {
        return slab.getSurfaceFinish() == null ? "Cilalı" : slab.getSurfaceFinish().getDisplayName();
    }
}
