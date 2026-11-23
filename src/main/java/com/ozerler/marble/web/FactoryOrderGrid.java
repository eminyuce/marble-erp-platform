package com.ozerler.marble.web;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.WorkOrderView;

/**
 * Fabrika iş emirleri. Status filter already exists. Search uses the existing work-order query.
 */
public final class FactoryOrderGrid {

    private FactoryOrderGrid() {
    }

    public static DataGridView view(Page<WorkOrderView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "factory-orders-table", columns(), WorkOrderGridRows::factory);
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, GridWidth.PX_150, "status", WorkOrderGridRows.statuses()),
                GridColumn.text("id", "Id", false, GridWidth.PX_110),
                GridColumn.text("orderNo", "İş Emri No", false, GridWidth.PX_160),
                GridColumn.text("customerName", "Müşteri", false, GridWidth.PX_180),
                GridColumn.text("stoneType", "Taş Türü", false, GridWidth.PX_150),
                GridColumn.text("quantity", "Miktar", false, GridWidth.PX_130),
                GridColumn.text("orderDate", "Sipariş Tarihi", false, GridWidth.PX_140),
                GridColumn.text("targetDeliveryDate", "Termin Tarihi", false, GridWidth.PX_140)
        );
    }
}
