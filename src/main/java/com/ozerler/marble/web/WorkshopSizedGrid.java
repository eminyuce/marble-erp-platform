package com.ozerler.marble.web;

import com.ozerler.marble.model.StockItem;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Atölye ebatlı stok. The row action opens yeniden ebatlama for the selected item.
 */
public final class WorkshopSizedGrid {

    private WorkshopSizedGrid() {
    }

    public static DataGridView view(Page<StockItem> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "workshop-sized-table", columns(), WorkshopSizedGrid::row);
    }

    static GridRow row(StockItem item) {
        String stone = OperationGridSupport.named(item.getStoneType(), "Mermer");
        String surface = item.getSurfaceFinish() == null ? "Cilalı" : item.getSurfaceFinish().getDisplayName();
        String description = OperationGridSupport.named(item.getDescription(), stone);
        String recutUrl = "/operations/workshop/sized/new?sourceStockItemId=" + item.getId();
        return GridRow.of(recutUrl, null, null, null, List.of(
                GridCell.badge("Stokta"),
                GridCell.chip(item.getId()),
                GridCell.text(item.getItemCode()),
                GridCell.text(item.getSourceBlock() == null ? "" : item.getSourceBlock().getBlockCode()),
                OperationGridSupport.lines(description, stone + " · " + surface),
                GridCell.text(OperationGridSupport.centimeters(item.getThicknessCm())),
                GridCell.text(OperationGridSupport.rectangle(item.getWidthCm(), item.getLengthCm())),
                GridCell.text(OperationGridSupport.amount(item.getQuantity(), "m²")),
                GridCell.text(item.getPieceCount() == null ? "" : item.getPieceCount().toString()),
                GridCell.text(OperationGridSupport.customer(item.getCustomer() == null ? null : item.getCustomer().getCompanyName()))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("status", "Durum", false, 130),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("itemCode", "Stok Kodu", false, 140),
                GridColumn.text("sourceBlockCode", "Kaynak Blok", false, 140),
                GridColumn.text("description", "Taş Türü & Tanım", false, 200),
                GridColumn.text("thicknessCm", "Kalınlık", false, 110),
                GridColumn.text("dimensions", "Ölçü", false, 140),
                GridColumn.text("quantity", "Miktar (M²)", false, 130),
                GridColumn.text("pieceCount", "Adet", false, 90),
                GridColumn.text("customerName", "Müşteri", false, 180)
        );
    }
}
