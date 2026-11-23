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
        return GridAssembler.from(response, page, size, sort, direction, "blocks-table",
                GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
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
        String deleteKey;
        if (deleteId == null) {
            deleteKey = "";
        } else {
            deleteKey = String.valueOf(deleteId);
        }
        return new GridRow(viewUrl, editUrl, deleteKey, block.getBlockCode(),
                actions(block, viewUrl, editUrl, deleteId), List.of(
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
                GridColumn.filtered("status", "Durum", true, GridWidth.PX_150, "status", statuses()),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("blockCode", "Blok Kodu", true, GridWidth.PX_160),
                GridColumn.text("quarryName", "Ocak", true, GridWidth.PX_140),
                GridColumn.text("extractionDate", "Oluşturma Tarihi", false, GridWidth.PX_140),
                GridColumn.text("locationName", "Saha", false, GridWidth.PX_140),
                GridColumn.text("customerName", "Müşteri", false, GridWidth.PX_180),
                GridColumn.text("quarrySection", "Ocak Bölgesi", false, GridWidth.PX_130),
                GridColumn.text("stoneType", "Taş Cinsi", true, GridWidth.PX_130),
                GridColumn.text("colorTone", "Seleksiyon", false, GridWidth.PX_120),
                GridColumn.text("estimatedTonnage", "Tonaj", false, GridWidth.PX_150),
                GridColumn.text("", "m²", false, GridWidth.PX_110),
                GridColumn.text("unitMarketValuePerTon", "Piyasa Değeri", false, GridWidth.PX_150),
                GridColumn.text("extractionCost", "Çıkarma Maliyeti", false, GridWidth.PX_150),
                GridColumn.text("totalCost", "Toplam Maliyet", true, GridWidth.PX_150)
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

    private static List<GridAction> actions(BlockDto block, String viewUrl, String editUrl, Object deleteId) {
        List<GridAction> items = new ArrayList<>();
        items.add(GridAction.link("Detay ve işlemler", "eye", viewUrl));
        items.add(GridAction.link("Düzenle", "edit-3", editUrl));
        if (atQuarry(block)) {
            Object tonnage = block.getApproximateTonnage();
            if (tonnage == null) {
                tonnage = 0;
            }
            items.add(GridAction.command(
                    "Kantar Tartımı Gir",
                    "scale",
                    GridAction.jsCall("openActualTonnageModal", block.getId(), block.getBlockCode(), tonnage)));
            if (!"PRODUCTION_YARD".equals(block.getLocationType())) {
                items.add(GridAction.link(
                        "Üretim Sahasına taşı",
                        "pickaxe",
                        viewUrl + "/move?targetType=PRODUCTION_YARD"));
            }
            if (!"DISPATCH_YARD".equals(block.getLocationType())) {
                items.add(GridAction.link(
                        "Stok Sahasına taşı",
                        "warehouse",
                        viewUrl + "/move?targetType=DISPATCH_YARD"));
            }
            Object price = block.getSalePrice();
            if (price == null) {
                price = block.getMarketValue();
            }
            if (price == null) {
                price = 0;
            }
            items.add(GridAction.command(
                    "Markala (Rezerve Et)",
                    "bookmark",
                    GridAction.jsCall(
                            "openMarkBlockModal",
                            block.getId(),
                            block.getBlockCode(),
                            price,
                            block.getAssignedCustomerId())));
            if ("DISPATCH_YARD".equals(block.getLocationType())) {
                items.add(GridAction.link("Fabrikaya sevk", "truck", viewUrl + "/transfer-to-factory"));
            }
            items.add(GridAction.link("Sat (Fatura Kes)", "receipt", viewUrl + "/sell"));
        }
        if (deleteId != null) {
            items.add(GridAction.delete("Bloğu Sil", deleteId, block.getBlockCode()));
        }
        return items;
    }

    private static boolean atQuarry(BlockDto block) {
        String canonical = block.getCanonicalStatus();
        String status = block.getStatus();
        boolean sold = "SOLD".equals(canonical) || "SOLD".equals(status);
        if (sold) {
            return false;
        }
        return "PRODUCED".equals(canonical)
                || "READY_FOR_DISPATCH".equals(canonical)
                || "MARKED".equals(canonical)
                || "QUARRY".equals(status)
                || "PRODUCED".equals(status)
                || "READY_FOR_DISPATCH".equals(status)
                || "MARKED".equals(status);
    }
}
