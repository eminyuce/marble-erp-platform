package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.PurchaseOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;

/**
 * Satın alma siparişleri. Sort keys match {@code GridPages.PURCHASE_ORDER_SORTS}.
 */
public final class ProcurementGrid {

    private ProcurementGrid() {
    }

    public static DataGridView view(TabulatorResponse<PurchaseOrderDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (PurchaseOrderDto order : response.getData()) {
                rows.add(row(order));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "procurement-table", 4, columns(), rows);
    }

    public static GridRow row(PurchaseOrderDto order) {
        String id;
        if (order.getId() == null) {
            id = "";
        } else {
            id = order.getId().toString();
        }
        String viewUrl = "/procurement/" + id;
        String invoiceUrl = "/invoices/new?type=PURCHASE&purchaseOrderId=" + id;
        return GridRow.of(
                viewUrl,
                viewUrl,
                null,
                order.getPoNumber(),
                List.of(
                        GridCell.badge(order.getStatusLabel()),
                        GridCell.chip(order.getId()),
                        GridCell.link(order.getPoNumber(), viewUrl),
                        GridCell.link(
                                GridWindow.clean(order.getSupplierName()),
                                viewUrl,
                                GridWindow.clean(order.getSupplierType()),
                                null),
                        GridCell.link(
                                GridWindow.clean(order.getProjectName()),
                                null,
                                GridWindow.clean(order.getBusinessUnitLabel()),
                                null),
                        GridCell.text(GridTexts.date(order.getOrderDate())),
                        GridCell.text(GridTexts.money(order.getTotalAmount())),
                        GridCell.link("Faturalaştır", invoiceUrl)));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", true, GridWidth.PX_150, "status", statuses()),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("poNumber", "Sipariş No", true, GridWidth.PX_160),
                GridColumn.text("supplierName", "Tedarikçi", true, GridWidth.PX_220),
                GridColumn.text("projectName", "Proje / Birim", true, GridWidth.PX_180),
                GridColumn.text("orderDate", "Sipariş Tarihi", true, GridWidth.PX_140),
                GridColumn.text("totalAmount", "Toplam Tutar", true, GridWidth.PX_150),
                GridColumn.text("", "Fatura", false, GridWidth.PX_140)
        );
    }

    private static List<GridColumn.FilterChoice> statuses() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (PurchaseOrderStatus status : PurchaseOrderStatus.values()) {
            choices.add(new GridColumn.FilterChoice(status.name(), status.getLabel()));
        }
        return choices;
    }
}
