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
        String id;
        if (block.getId() == null) {
            id = "";
        } else {
            id = block.getId().toString();
        }
        String viewUrl = "/blocks/" + id;
        String editUrl = viewUrl + "/edit";
        Object deleteId;
        if (block.isCanDelete()) {
            deleteId = block.getId();
        } else {
            deleteId = null;
        }
        BigDecimal extraction;
        if (block.getCalculatedExtractionCost() != null) {
            extraction = block.getCalculatedExtractionCost();
        } else {
            extraction = block.getExtractionCost();
        }
        String link;
        if (block.isWeightDeviationWarning()) {
            link = "%5 sapma uyarısı";
        } else {
            link = "";
        }
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
                GridCell.link(tonnage(block), null, link, null),
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
        String price;
        if (block.getSalePrice() != null && block.getSalePrice().signum() > 0) {
            price = " (" + GridTexts.money(block.getSalePrice()) + ")";
        } else {
            price = "";
        }
        return "Rezerve: " + assigned + price;
    }

    private static String tonnage(BlockDto block) {
        String approximate = GridTexts.quantity(block.getApproximateTonnage());
        if (block.getActualTonnage() != null && block.getActualTonnage().signum() > 0) {
            return approximate + " / " + GridTexts.quantity(block.getActualTonnage()) + " t";
        }
        if (approximate.isBlank()) {
            return "";
        }
        return approximate + " t";
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
