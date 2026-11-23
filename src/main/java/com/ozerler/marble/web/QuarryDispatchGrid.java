package com.ozerler.marble.web;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.InvoiceRef;
import com.ozerler.marble.dto.view.StockMovementView;

/**
 * Ocak sevkiyat raporu. The movement query has no search or status filter. Invoice is not fetched; the service copies it only when it is already loaded.
 */
public final class QuarryDispatchGrid {

    private QuarryDispatchGrid() {
    }

    public static DataGridView view(Page<StockMovementView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "quarry-dispatches-table", columns(), QuarryDispatchGrid::row);
    }

    static GridRow row(StockMovementView movement) {
        String product = productName(movement);
        String blockUrl = movement.getBlock() == null ? null : "/blocks/" + movement.getBlock().getId();
        String target = movement.getTargetDestination() != null
                ? movement.getTargetDestination().getDisplayName()
                : movement.getTargetDepartment() == null ? "Fabrika" : movement.getTargetDepartment().getDisplayName();
        String customer = movement.getCustomer() == null ? "" : movement.getCustomer().getCompanyName();
        return GridRow.of(blockUrl, null, null, null, List.of(
                GridCell.chip(movement.getId()),
                blockUrl == null ? GridCell.text(product) : GridCell.link(product, blockUrl),
                GridCell.text(OperationGridSupport.day(movement.getMovementDate())),
                GridCell.text(measure(movement)),
                GridCell.badge("Ocak"),
                GridCell.badge(target),
                GridCell.text(customer),
                invoice(movement),
                GridCell.text(movement.getNotes())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("productName", "Ürün / Blok", false, 160),
                GridColumn.text("movementDate", "Tarih", false, 160),
                GridColumn.text("tonnage", "Tonaj / Miktar", false, 150),
                GridColumn.text("source", "Kaynak", false, 110),
                GridColumn.text("targetDestination", "Hedef", false, 140),
                GridColumn.text("customerName", "Müşteri", false, 180),
                GridColumn.text("invoiceNo", "Fatura", false, 140),
                GridColumn.text("notes", "Açıklama", false, 200)
        );
    }

    private static GridCell invoice(StockMovementView movement) {
        InvoiceRef invoice = movement.getInvoice();
        if (invoice == null) {
            return GridCell.text("");
        }
        return GridCell.link(invoice.getInvoiceNo(), "/invoices/" + invoice.getId());
    }

    private static String productName(StockMovementView movement) {
        if (movement.getBlock() != null && movement.getBlock().getBlockCode() != null) {
            return movement.getBlock().getBlockCode();
        }
        return OperationGridSupport.named(movement.getItemDescription(), "Mermer Blok");
    }

    private static String measure(StockMovementView movement) {
        if (movement.getTonnage() != null) {
            return OperationGridSupport.amount(movement.getTonnage(), "ton");
        }
        String unit = movement.getQuantityUnit() == null ? "ton" : movement.getQuantityUnit();
        return OperationGridSupport.amount(movement.getQuantity(), unit);
    }
}
