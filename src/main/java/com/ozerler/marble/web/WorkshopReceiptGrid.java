package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.WorkshopReceiptDto;

/**
 * Atölye malzeme kabulleri. Rows are already loaded and paged in memory. Columns are not sortable.
 */
public final class WorkshopReceiptGrid {

    private WorkshopReceiptGrid() {
    }

    public static DataGridView view(List<WorkshopReceiptDto> receipts, int page, int size, String search) {
        List<WorkshopReceiptDto> matched = new ArrayList<>();
        GridColumnFilters filters = GridColumnFilters.fromCurrentRequest(
                GridFilterCatalog.aliases("workshop-receipts-table"));
        if (receipts != null) {
            for (WorkshopReceiptDto receipt : receipts) {
                if (GridWindow.matches(search, receipt.receiptNo(), receipt.sourceLabel(), receipt.supplierName(),
                        receipt.stoneType(), receipt.notes())
                        && GridColumnFilterMatcher.matches(filters, key -> columnValue(receipt, key))) {
                    matched.add(receipt);
                }
            }
        }
        List<GridRow> rows = new ArrayList<>();
        for (WorkshopReceiptDto receipt : GridWindow.page(matched, page, size)) {
            rows.add(row(receipt));
        }
        return GridAssembler.from(page, size, GridWindow.pageCount(matched.size(), size), matched.size(),
                null, null, "workshop-receipts-table", GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
    }

    private static GridRow row(WorkshopReceiptDto receipt) {
        return GridRow.of("", "", null, receipt.receiptNo(), List.of(
                GridCell.text(GridWindow.clean(receipt.receiptNo())),
                GridCell.text(GridWindow.clean(receipt.supplierName())),
                GridCell.text(GridWindow.clean(receipt.sourceLabel())),
                GridCell.text(GridWindow.clean(receipt.stoneType())),
                GridCell.text(GridTexts.quantity(receipt.quantity())),
                GridCell.text(GridWindow.withUnit(receipt.areaM2(), "m²")),
                GridCell.text(GridTexts.money(receipt.purchaseCost())),
                GridCell.text(GridWindow.isoDate(receipt.receivedAt())),
                GridCell.text(GridWindow.clean(receipt.notes()))
        ));
    }

    private static String columnValue(WorkshopReceiptDto receipt, String key) {
        if (receipt == null || key == null) {
            return "";
        }
        return switch (key) {
            case "receiptNo" -> receipt.receiptNo();
            case "supplierName" -> receipt.supplierName();
            case "sourceLabel" -> receipt.sourceLabel();
            case "stoneType" -> receipt.stoneType();
            case "quantity" -> text(receipt.quantity());
            case "areaM2" -> text(receipt.areaM2());
            case "purchaseCost" -> text(receipt.purchaseCost());
            case "receivedAt" -> receipt.receivedAt();
            case "notes" -> receipt.notes();
            default -> "";
        };
    }

    private static String text(java.math.BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.toPlainString();
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("receiptNo", "Kabul No", false, GridWidth.PX_150),
                GridColumn.text("supplierName", "Tedarikçi", false, GridWidth.PX_160),
                GridColumn.text("sourceLabel", "Kaynak", false, GridWidth.PX_140),
                GridColumn.text("stoneType", "Taş cinsi", false, GridWidth.PX_140),
                GridColumn.text("quantity", "Miktar", false, GridWidth.PX_110),
                GridColumn.text("areaM2", "m²", false, GridWidth.PX_110),
                GridColumn.text("purchaseCost", "Alış maliyeti", false, GridWidth.PX_140),
                GridColumn.text("receivedAt", "Tarih", false, GridWidth.PX_130),
                GridColumn.text("notes", "Not", false, GridWidth.PX_180)
        );
    }
}
