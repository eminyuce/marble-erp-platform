package com.ozerler.marble.web;

import com.ozerler.marble.dto.SalesOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.SalesOrderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Satış siparişleri. Sort keys match {@code GridPages.SALES_ORDER_SORTS}.
 */
public final class SalesGrid {

    private SalesGrid() {
    }

    public static DataGridView view(TabulatorResponse<SalesOrderDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (SalesOrderDto order : response.getData()) {
                rows.add(row(order));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "sales-table", 4, columns(), rows);
    }

    public static GridRow row(SalesOrderDto order) {
        String id = order.getId() == null ? "" : order.getId().toString();
        String viewUrl = "/sales/" + id;
        return GridRow.of(viewUrl, viewUrl, null, order.getOrderNo(), List.of(
                GridCell.badge(order.getStatusLabel()),
                GridCell.chip(order.getId()),
                GridCell.link(order.getOrderNo(), viewUrl),
                GridCell.link(GridWindow.clean(order.getCustomerName()), viewUrl, GridWindow.clean(order.getCustomerType()), null),
                GridCell.text(GridTexts.date(order.getOrderDate())),
                GridCell.text(GridTexts.money(order.getTotalAmount())),
                GridCell.text(GridTexts.money(order.getPaidAmount()))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", true, 150, "status", statuses()),
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("orderNo", "Sipariş No", true, 160),
                GridColumn.text("customerName", "Müşteri", true, 220),
                GridColumn.text("orderDate", "Sipariş Tarihi", true, 140),
                GridColumn.text("totalAmount", "Toplam Tutar", true, 150),
                GridColumn.text("paidAmount", "Ödenen", true, 140)
        );
    }

    private static List<GridColumn.FilterChoice> statuses() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (SalesOrderStatus status : SalesOrderStatus.values()) {
            choices.add(new GridColumn.FilterChoice(status.name(), status.getLabel()));
        }
        return choices;
    }
}
