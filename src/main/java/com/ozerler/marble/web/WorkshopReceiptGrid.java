package com.ozerler.marble.web;

import com.ozerler.marble.dto.WorkshopReceiptDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Atölye malzeme kabulleri. Rows are already loaded and paged in memory. Columns are not sortable.
 */
public final class WorkshopReceiptGrid {

    private WorkshopReceiptGrid() {
    }

    public static DataGridView view(List<WorkshopReceiptDto> receipts, int page, int size, String search) {
        List<WorkshopReceiptDto> matched = new ArrayList<>();
        if (receipts != null) {
            for (WorkshopReceiptDto receipt : receipts) {
                if (GridWindow.matches(search, receipt.receiptNo(), receipt.sourceLabel(), receipt.supplierName(),
                        receipt.stoneType(), receipt.notes())) {
                    matched.add(receipt);
                }
            }
        }
        List<GridRow> rows = new ArrayList<>();
        for (WorkshopReceiptDto receipt : GridWindow.page(matched, page, size)) {
            rows.add(row(receipt));
        }
        return GridAssembler.from(page, size, GridWindow.pageCount(matched.size(), size), matched.size(),
                null, null, "workshop-receipts-table", 4, columns(), rows);
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

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("", "Kabul No", false, 150),
                GridColumn.text("", "Tedarikçi", false, 160),
                GridColumn.text("", "Kaynak", false, 140),
                GridColumn.text("", "Taş cinsi", false, 140),
                GridColumn.text("", "Miktar", false, 110),
                GridColumn.text("", "m²", false, 110),
                GridColumn.text("", "Alış maliyeti", false, 140),
                GridColumn.text("", "Tarih", false, 130),
                GridColumn.text("", "Not", false, 180)
        );
    }
}
