package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BlockStatus;

/**
 * Ocak blok listesi. Sort keys match {@code GridPages.BLOCK_SORTS}. Status filter already exists.
 */
public final class BlockGrid {

    private BlockGrid() {
    }

    public static DataGridView view(TabulatorResponse<BlockDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (BlockDto block : response.getData()) {
                rows.add(row(block));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "blocks-table", 4, columns(), rows);
    }

    public static GridRow row(BlockDto block) {
        String id = block.getId() == null ? "" : block.getId().toString();
        String viewUrl = "/blocks/" + id;
        String editUrl = viewUrl + "/edit";
        Object deleteId = block.isCanDelete() ? block.getId() : null;
        BigDecimal extraction = block.getCalculatedExtractionCost() != null
                ? block.getCalculatedExtractionCost()
                : block.getExtractionCost();
        return GridRow.of(viewUrl, editUrl, deleteId, block.getBlockCode(), List.of(
                GridCell.badge(block.getStatusLabel()),
                GridCell.chip(block.getId()),
                GridCell.link(block.getBlockCode(), viewUrl),
                GridCell.link(GridWindow.clean(block.getQuarryName()), viewUrl),
                GridCell.text(recordedOn(block)),
                GridCell.text(GridWindow.clean(block.getLocationName())),
                GridCell.text(customer(block)),
                GridCell.text(GridWindow.clean(block.getQuarrySection())),
                GridCell.text(GridWindow.clean(block.getStoneType())),
                GridCell.text(GridWindow.clean(block.getColorTone())),
                GridCell.link(tonnage(block), null, block.isWeightDeviationWarning() ? "%5 sapma uyarısı" : "", null),
                GridCell.text(GridWindow.withUnit(block.getSurfaceAreaM2(), "m²")),
                GridCell.text(market(block)),
                GridCell.text(GridTexts.money(extraction)),
                GridCell.text(GridTexts.money(block.getTotalCost()))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", true, 150, "status", statuses()),
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("blockCode", "Blok Kodu", true, 160),
                GridColumn.text("quarryName", "Ocak", true, 140),
                GridColumn.text("", "Oluşturma Tarihi", false, 140),
                GridColumn.text("", "Saha", false, 140),
                GridColumn.text("", "Müşteri", false, 180),
                GridColumn.text("", "Ocak Bölgesi", false, 130),
                GridColumn.text("stoneType", "Taş Cinsi", true, 130),
                GridColumn.text("", "Seleksiyon", false, 120),
                GridColumn.text("", "Tonaj", false, 150),
                GridColumn.text("", "m²", false, 110),
                GridColumn.text("", "Piyasa Değeri", false, 150),
                GridColumn.text("", "Çıkarma Maliyeti", false, 150),
                GridColumn.text("totalCost", "Toplam Maliyet", true, 150)
        );
    }

    private static List<GridColumn.FilterChoice> statuses() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (BlockStatus status : BlockStatus.values()) {
            choices.add(new GridColumn.FilterChoice(status.name(), status.getLabel()));
        }
        return choices;
    }

    private static String recordedOn(BlockDto block) {
        if (block.getExtractionDate() != null) {
            return GridTexts.date(block.getExtractionDate());
        }
        return GridTexts.dateTime(block.getCreatedAt());
    }

    private static String customer(BlockDto block) {
        boolean sold = "SOLD".equals(block.getCanonicalStatus()) || "SOLD".equals(block.getStatus());
        if (sold) {
            return GridWindow.clean(block.getSoldCustomerName());
        }
        String assigned = GridWindow.clean(block.getAssignedCustomerName());
        if (assigned.isBlank()) {
            return "";
        }
        String price = block.getSalePrice() != null && block.getSalePrice().signum() > 0
                ? " (" + GridTexts.money(block.getSalePrice()) + ")"
                : "";
        return "Rezerve: " + assigned + price;
    }

    private static String tonnage(BlockDto block) {
        String approximate = GridTexts.quantity(block.getApproximateTonnage());
        if (block.getActualTonnage() != null && block.getActualTonnage().signum() > 0) {
            return approximate + " / " + GridTexts.quantity(block.getActualTonnage()) + " t";
        }
        return approximate.isBlank() ? "" : approximate + " t";
    }

    private static String market(BlockDto block) {
        if (block.getMarketValue() != null) {
            return GridTexts.money(block.getMarketValue());
        }
        if (block.getUnitMarketValuePerTon() != null) {
            return GridTexts.money(block.getUnitMarketValuePerTon()) + "/ton";
        }
        return "";
    }
}
