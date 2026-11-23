package com.ozerler.marble.web;

import com.ozerler.marble.dto.view.WorkOrderView;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Combined work-order list. Status filter already exists. Department stays a query parameter.
 */
public final class WorkOrderGrid {

    private WorkOrderGrid() {
    }

    public static DataGridView view(Page<WorkOrderView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "workorders-table", columns(), WorkOrderGridRows::combined);
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, 160, "status", WorkOrderGridRows.statuses()),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("orderNo", "İş Emri No", false, 170),
                GridColumn.text("departmentLabel", "Departman", false, 140),
                GridColumn.text("customerName", "Müşteri", false, 180),
                GridColumn.text("stoneType", "Taş Türü", false, 150),
                GridColumn.text("quantity", "Miktar", false, 130),
                GridColumn.text("targetDeliveryDate", "Termin Tarihi", false, 140),
                GridColumn.text("responsiblePerson", "Sorumlu", false, 150)
        );
    }
}
