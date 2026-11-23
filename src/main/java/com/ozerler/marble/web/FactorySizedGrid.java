package com.ozerler.marble.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.StockItemView;

/**
 * Fabrika ebatlı stok. There is no status query parameter, so the status column is not filtered.
 */
public final class FactorySizedGrid {

    private FactorySizedGrid() {
    }

    public static DataGridView view(Page<StockItemView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "factory-sized-table", columns(), FactorySizedGrid::row);
    }

    static GridRow row(StockItemView item) {
        String description = OperationGridSupport.named(item.getDescription(),
                OperationGridSupport.named(item.getStoneType(), "Ebatlı Mermer"));
        String stone = OperationGridSupport.named(item.getStoneType(), "Mermer");
        String surface;
        if (item.getSurfaceFinish() == null) {
            surface = "Cilalı";
        } else {
            surface = item.getSurfaceFinish().getDisplayName();
        }
        String edge = OperationGridSupport.named(item.getEdgeFinish(), "Düz");
        String lines;
        if (stone.equals(description)) {
            lines = "";
        } else {
            lines = stone;
        }
        String value;
        if (item.getPieceCount() == null) {
            value = "";
        } else {
            value = item.getPieceCount().toString();
        }
        String customer;
        if (item.getCustomer() == null) {
            customer = null;
        } else {
            customer = item.getCustomer().getCompanyName();
        }
        return GridRow.of(null, null, null, null, List.of(
                GridCell.badge("Hazır / Stokta"),
                GridCell.chip(item.getId()),
                GridCell.text(item.getItemCode()),
                GridCell.text(produced(item)),
                GridCell.text(sourceLabel(item)),
                OperationGridSupport.lines(description, lines),
                OperationGridSupport.lines(surface, edge),
                GridCell.text(OperationGridSupport.centimeters(item.getThicknessCm())),
                GridCell.text(OperationGridSupport.rectangle(item.getWidthCm(), item.getLengthCm())),
                GridCell.text(OperationGridSupport.amount(item.getQuantity(), "m²")),
                GridCell.text(value),
                GridCell.text(GridTexts.quantity(item.getActualProducedQuantity())),
                GridCell.text(OperationGridSupport.customer(customer))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("status", "Durum", false, 150),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("itemCode", "Ürün Kodu", false, 140),
                GridColumn.text("productionDate", "Üretim Tarihi", false, 140),
                GridColumn.text("sourceBlockCode", "Kaynak", false, 140),
                GridColumn.text("description", "Taş Türü & Tanım", false, 200),
                GridColumn.text("surfaceFinish", "Yüzey & Kenar", false, 150),
                GridColumn.text("thicknessCm", "Kalınlık", false, 110),
                GridColumn.text("dimensions", "Ölçü", false, 140),
                GridColumn.text("quantity", "Miktar (M²)", false, 130),
                GridColumn.text("pieceCount", "Adet", false, 90),
                GridColumn.text("actualProducedQuantity", "Gerçek Üretim", false, 140),
                GridColumn.text("customerName", "Müşteri", false, 180)
        );
    }

    private static String produced(StockItemView item) {
        LocalDate producedOn = item.getProductionDate();
        if (producedOn != null) {
            return GridTexts.date(producedOn);
        }
        return OperationGridSupport.day(item.getCreatedDate());
    }

    private static String sourceLabel(StockItemView item) {
        if (item.getSourceBlock() != null && item.getSourceBlock().getBlockCode() != null
                && !item.getSourceBlock().getBlockCode().isBlank()) {
            return item.getSourceBlock().getBlockCode();
        }
        String notes = item.getNotes();
        if (notes != null) {
            int marker = notes.indexOf("[Kaynak Plaka:");
            int end;
            if (marker < 0) {
                end = -1;
            } else {
                end = notes.indexOf(']', marker);
            }
            if (marker >= 0 && end > marker) {
                String code = notes.substring(marker + "[Kaynak Plaka:".length(), end).trim();
                if (code.isEmpty()) {
                    return "Plaka";
                }
                return code;
            }
        }
        return "Bağımsız";
    }
}
