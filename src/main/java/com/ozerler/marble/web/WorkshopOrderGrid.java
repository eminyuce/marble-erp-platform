package com.ozerler.marble.web;

import com.ozerler.marble.dto.view.WorkOrderView;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Atölye iş emirleri. Status filter already exists.
 */
public final class WorkshopOrderGrid {

    private WorkshopOrderGrid() {
    }

    public static DataGridView view(Page<WorkOrderView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "workshop-orders-table", columns(), WorkOrderGridRows::workshop);
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, 150, "status", WorkOrderGridRows.statuses()),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("orderNo", "İş Emri No", false, 160),
                GridColumn.text("customerName", "Müşteri / Proje", false, 180),
                GridColumn.text("stoneType", "Taş Cinsi", false, 150),
                GridColumn.text("quantity", "Miktar", false, 130),
                GridColumn.text("responsiblePerson", "Sorumlu Usta", false, 150),
                GridColumn.text("orderDate", "Sipariş Tarihi", false, 140),
                GridColumn.text("targetDeliveryDate", "Termin Tarihi", false, 140)
        );
    }
}
