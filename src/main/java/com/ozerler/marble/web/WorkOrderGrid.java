package com.ozerler.marble.web;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.WorkOrderView;

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
                GridColumn.filtered("status", "Durum", false, GridWidth.PX_160, "status", WorkOrderGridRows.statuses()),
                GridColumn.text("id", "Id", false, GridWidth.PX_110),
                GridColumn.text("orderNo", "İş Emri No", false, GridWidth.PX_170),
                GridColumn.text("departmentLabel", "Departman", false, GridWidth.PX_140),
                GridColumn.text("customerName", "Müşteri", false, GridWidth.PX_180),
                GridColumn.text("stoneType", "Taş Türü", false, GridWidth.PX_150),
                GridColumn.text("quantity", "Miktar", false, GridWidth.PX_130),
                GridColumn.text("targetDeliveryDate", "Termin Tarihi", false, GridWidth.PX_140),
                GridColumn.text("responsiblePerson", "Sorumlu", false, GridWidth.PX_150)
        );
    }
}
