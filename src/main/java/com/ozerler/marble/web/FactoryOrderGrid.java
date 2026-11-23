package com.ozerler.marble.web;

import com.ozerler.marble.dto.view.WorkOrderView;
import org.springframework.data.domain.Page;

import java.util.List;

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
                GridColumn.filtered("status", "Durum", false, 150, "status", WorkOrderGridRows.statuses()),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("orderNo", "İş Emri No", false, 160),
                GridColumn.text("customerName", "Müşteri", false, 180),
                GridColumn.text("stoneType", "Taş Türü", false, 150),
                GridColumn.text("quantity", "Miktar", false, 130),
                GridColumn.text("orderDate", "Sipariş Tarihi", false, 140),
                GridColumn.text("targetDeliveryDate", "Termin Tarihi", false, 140)
        );
    }
}
