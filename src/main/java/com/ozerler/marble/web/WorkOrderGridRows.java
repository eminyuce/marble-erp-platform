package com.ozerler.marble.web;

import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cells shared by the factory, workshop, and combined work-order grids.
 */
final class WorkOrderGridRows {

    private WorkOrderGridRows() {
    }

    static String url(OperationWorkOrder order) {
        return "/work-orders/" + order.getId();
    }

    static GridRow factory(OperationWorkOrder order) {
        List<GridCell> cells = lead(order);
        cells.add(ordered(order));
        cells.add(due(order));
        return row(order, cells);
    }

    static GridRow workshop(OperationWorkOrder order) {
        List<GridCell> cells = lead(order);
        cells.add(person(order));
        cells.add(ordered(order));
        cells.add(due(order));
        return row(order, cells);
    }

    static GridRow combined(OperationWorkOrder order) {
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

    static GridRow row(OperationWorkOrder order, List<GridCell> cells) {
        return GridRow.of(url(order), null, null, null, cells);
    }

    private static List<GridCell> lead(OperationWorkOrder order) {
        List<GridCell> cells = new ArrayList<>();
        cells.add(status(order));
        cells.add(GridCell.chip(order.getId()));
        cells.add(code(order));
        cells.add(customer(order));
        cells.add(stone(order));
        cells.add(quantity(order));
        return cells;
    }

    static GridCell status(OperationWorkOrder order) {
        return GridCell.badge(order.getStatus() == null ? "" : order.getStatus().getDisplayName());
    }

    static GridCell code(OperationWorkOrder order) {
        return GridCell.link(order.getOrderNo(), url(order));
    }

    static GridCell customer(OperationWorkOrder order) {
        return GridCell.text(order.getCustomerDisplayName());
    }

    static GridCell stone(OperationWorkOrder order) {
        return GridCell.text(order.getStoneType());
    }

    static GridCell quantity(OperationWorkOrder order) {
        BigDecimal quantity = order.getQuantity() == null ? BigDecimal.ZERO : order.getQuantity();
        String unit = order.getUnit() == null ? "m²" : order.getUnit();
        return GridCell.text(OperationGridSupport.amount(quantity, unit));
    }

    static GridCell person(OperationWorkOrder order) {
        return GridCell.text(order.getResponsiblePerson());
    }

    static GridCell ordered(OperationWorkOrder order) {
        return GridCell.text(OperationGridSupport.day(order.getOrderDate()));
    }

    static GridCell due(OperationWorkOrder order) {
        return GridCell.text(OperationGridSupport.day(order.getDueDate()));
    }

    static GridCell department(OperationWorkOrder order) {
        return GridCell.text(order.getDepartment() == null ? "" : order.getDepartment().getDisplayName());
    }

    static List<GridColumn.FilterChoice> statuses() {
        return OperationGridSupport.choices(OperationWorkOrderStatus.values(), OperationWorkOrderStatus::getDisplayName);
    }
}
