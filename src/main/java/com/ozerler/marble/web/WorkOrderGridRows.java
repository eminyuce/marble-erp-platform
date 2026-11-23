package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.view.WorkOrderView;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;

/**
 * Cells shared by the factory, workshop, and combined work-order grids.
 */
final class WorkOrderGridRows {

    private WorkOrderGridRows() {
    }

    static String url(WorkOrderView order) {
        return "/work-orders/" + order.getId();
    }

    static GridRow factory(WorkOrderView order) {
        List<GridCell> cells = lead(order);
        cells.add(ordered(order));
        cells.add(due(order));
        return row(order, cells);
    }

    static GridRow workshop(WorkOrderView order) {
        List<GridCell> cells = lead(order);
        cells.add(person(order));
        cells.add(ordered(order));
        cells.add(due(order));
        return row(order, cells);
    }

    static GridRow combined(WorkOrderView order) {
        List<GridCell> cells = new ArrayList<>();
        cells.add(status(order));
        cells.add(GridCell.chip(order.getId()));
        cells.add(code(order));
        cells.add(department(order));
        cells.add(customer(order));
        cells.add(stone(order));
        cells.add(quantity(order));
        cells.add(due(order));
        cells.add(person(order));
        return row(order, cells);
    }

    static GridRow row(WorkOrderView order, List<GridCell> cells) {
        return GridRow.of(url(order), null, null, null, cells);
    }

    private static List<GridCell> lead(WorkOrderView order) {
        List<GridCell> cells = new ArrayList<>();
        cells.add(status(order));
        cells.add(GridCell.chip(order.getId()));
        cells.add(code(order));
        cells.add(customer(order));
        cells.add(stone(order));
        cells.add(quantity(order));
        return cells;
    }

    static GridCell status(WorkOrderView order) {
        String badge;
        if (order.getStatus() == null) {
            badge = "";
        } else {
            badge = order.getStatus().getDisplayName();
        }
        return GridCell.badge(badge);
    }

    static GridCell code(WorkOrderView order) {
        return GridCell.link(order.getOrderNo(), url(order));
    }

    static GridCell customer(WorkOrderView order) {
        return GridCell.text(order.getCustomerDisplayName());
    }

    static GridCell stone(WorkOrderView order) {
        return GridCell.text(order.getStoneType());
    }

    static GridCell quantity(WorkOrderView order) {
        BigDecimal quantity;
        if (order.getQuantity() == null) {
            quantity = BigDecimal.ZERO;
        } else {
            quantity = order.getQuantity();
        }
        String unit;
        if (order.getUnit() == null) {
            unit = "m²";
        } else {
            unit = order.getUnit();
        }
        return GridCell.text(OperationGridSupport.amount(quantity, unit));
    }

    static GridCell person(WorkOrderView order) {
        return GridCell.text(order.getResponsiblePerson());
    }

    static GridCell ordered(WorkOrderView order) {
        return GridCell.text(OperationGridSupport.day(order.getOrderDate()));
    }

    static GridCell due(WorkOrderView order) {
        return GridCell.text(OperationGridSupport.day(order.getDueDate()));
    }

    static GridCell department(WorkOrderView order) {
        String value;
        if (order.getDepartment() == null) {
            value = "";
        } else {
            value = order.getDepartment().getDisplayName();
        }
        return GridCell.text(value);
    }

    static List<GridColumn.FilterChoice> statuses() {
        return OperationGridSupport.choices(OperationWorkOrderStatus.values(), OperationWorkOrderStatus::getDisplayName);
    }
}
