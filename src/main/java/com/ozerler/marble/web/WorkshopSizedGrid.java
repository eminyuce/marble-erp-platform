package com.ozerler.marble.web;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.StockItemView;

/**
 * Atölye ebatlı stok. The row action opens yeniden ebatlama for the selected item.
 */
public final class WorkshopSizedGrid {

    private WorkshopSizedGrid() {
    }

    public static DataGridView view(Page<StockItemView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "workshop-sized-table", columns(), WorkshopSizedGrid::row);
    }

    static GridRow row(StockItemView item) {
        String stone = OperationGridSupport.named(item.getStoneType(), "Mermer");
        String surface;
        if (item.getSurfaceFinish() == null) {
            surface = "Cilalı";
        } else {
            surface = item.getSurfaceFinish().getDisplayName();
        }
        String description = OperationGridSupport.named(item.getDescription(), stone);
        String recutUrl = "/operations/workshop/sized/new?sourceStockItemId=" + item.getId();
        String value;
        if (item.getSourceBlock() == null) {
            value = "";
        } else {
            value = item.getSourceBlock().getBlockCode();
        }
        String valueValue;
        if (item.getPieceCount() == null) {
            valueValue = "";
        } else {
            valueValue = item.getPieceCount().toString();
        }
        String customer;
        if (item.getCustomer() == null) {
            customer = null;
        } else {
            customer = item.getCustomer().getCompanyName();
        }
        return GridRow.of(recutUrl, null, null, null, List.of(
                GridCell.badge("Stokta"),
                GridCell.chip(item.getId()),
                GridCell.text(item.getItemCode()),
                GridCell.text(value),
                OperationGridSupport.lines(description, stone + " · " + surface),
                GridCell.text(OperationGridSupport.centimeters(item.getThicknessCm())),
                GridCell.text(OperationGridSupport.rectangle(item.getWidthCm(), item.getLengthCm())),
                GridCell.text(OperationGridSupport.amount(item.getQuantity(), "m²")),
                GridCell.text(valueValue),
                GridCell.text(OperationGridSupport.customer(customer))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("status", "Durum", false, GridWidth.PX_130),
                GridColumn.text("id", "Id", false, GridWidth.PX_110),
                GridColumn.text("itemCode", "Stok Kodu", false, GridWidth.PX_140),
                GridColumn.text("sourceBlockCode", "Kaynak Blok", false, GridWidth.PX_140),
                GridColumn.text("description", "Taş Türü & Tanım", false, GridWidth.PX_200),
                GridColumn.text("thicknessCm", "Kalınlık", false, GridWidth.PX_110),
                GridColumn.text("dimensions", "Ölçü", false, GridWidth.PX_140),
                GridColumn.text("quantity", "Miktar (M²)", false, GridWidth.PX_130),
                GridColumn.text("pieceCount", "Adet", false, GridWidth.PX_90),
                GridColumn.text("customerName", "Müşteri", false, GridWidth.PX_180)
        );
    }
}
