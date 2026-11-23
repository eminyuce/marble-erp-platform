package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.dto.view.FactoryOperationView;

/**
 * Silim ve ebatlama geçmişi. The list is already loaded and paged in memory. Columns are not sortable.
 */
public final class PolishGrid {

    private PolishGrid() {
    }

    public static DataGridView view(List<FactoryOperationView> operations, int page, int size, String search) {
        List<OperationView> matched = new ArrayList<>();
        if (operations != null) {
            for (FactoryOperationView operation : operations) {
                OperationView view = OperationView.from(operation);
                if (GridWindow.matches(search, view.orderNo, view.blockCode, view.operatorName, view.machineName, view.processLabel)) {
                    matched.add(view);
                }
            }
        }
        List<GridRow> rows = new ArrayList<>();
        for (OperationView operation : GridWindow.page(matched, page, size)) {
            rows.add(row(operation));
        }
        return GridAssembler.from(page, size, GridWindow.pageCount(matched.size(), size), matched.size(),
                null, null, "polish-operations-grid", 4, columns(), rows);
    }

    private static GridRow row(OperationView operation) {
        String editUrl = "/production/polish/" + operation.id + "/edit";
        String orderUrl;
        if (operation.workOrderId == null) {
            orderUrl = null;
        } else {
            orderUrl = "/production/orders/" + operation.workOrderId;
        }
        return GridRow.of(editUrl, editUrl, null, operation.orderNo, List.of(
                GridCell.chip(operation.id),
                GridCell.link(operation.orderNo, orderUrl),
                GridCell.link(operation.operatorName, null, operation.machineName, null),
                GridCell.text(operation.blockCode),
                GridCell.text(operation.processLabel),
                GridCell.text(GridWindow.withUnit(operation.input, "m²")),
                GridCell.text(GridWindow.withUnit(operation.output, "m²")),
                GridCell.text(waste(operation.input, operation.waste)),
                GridCell.text(operation.chamfer),
                GridCell.text(GridTexts.money(operation.totalCost)),
                GridCell.text(GridTexts.dateTime(operation.finishedAt))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("", "İşlem No", false, GridWidth.PX_110),
                GridColumn.text("", "İş Emri", false, GridWidth.PX_140),
                GridColumn.text("", "Makine / Operatör", false, GridWidth.PX_180),
                GridColumn.text("", "Blok Kodu", false, GridWidth.PX_140),
                GridColumn.text("", "Operasyon Türü", false, GridWidth.PX_160),
                GridColumn.text("", "Giriş (m²)", false, GridWidth.PX_120),
                GridColumn.text("", "Çıkış (m²)", false, GridWidth.PX_120),
                GridColumn.text("", "Fire (m² / %)", false, GridWidth.PX_140),
                GridColumn.text("", "Pah Durumu", false, GridWidth.PX_130),
                GridColumn.text("", "Toplam Gider", false, GridWidth.PX_140),
                GridColumn.text("", "Tarih", false, GridWidth.PX_150)
        );
    }

    private static String waste(BigDecimal input, BigDecimal waste) {
        String amount = GridWindow.withUnit(waste, "m²");
        if (amount.isBlank() || input == null || input.signum() <= 0 || waste == null) {
            return amount;
        }
        BigDecimal percent = waste.multiply(BigDecimal.valueOf(100)).divide(input, 1, RoundingMode.HALF_UP);
        return amount + " (%" + percent.toPlainString() + ")";
    }

    private record OperationView(Long id, Long workOrderId, String orderNo, String blockCode, String processLabel,
                                 String operatorName, String machineName, BigDecimal input, BigDecimal output,
                                 BigDecimal waste, String chamfer, BigDecimal totalCost, LocalDateTime finishedAt) {

        private static OperationView from(FactoryOperationView operation) {
            var workOrder = operation.getWorkOrder();
            BlockView block;
            if (workOrder == null) {
                block = null;
            } else {
                block = workOrder.getBlock();
            }
            var machine = operation.getMachine();
            String process;
            if (operation.getProcessType() == null) {
                process = "";
            } else {
                process = operation.getProcessType().getLabel();
            }
            String chamferLabel;
            if (operation.getChamferStatus() == null) {
                chamferLabel = "";
            } else {
                chamferLabel = operation.getChamferStatus().getLabel();
            }
            Long value;
            if (workOrder == null) {
                value = null;
            } else {
                value = workOrder.getId();
            }
            String clean;
            if (workOrder == null) {
                clean = "";
            } else {
                clean = workOrder.getOrderNo();
            }
            String cleanValue;
            if (block == null) {
                cleanValue = "";
            } else {
                cleanValue = block.getBlockCode();
            }
            String cleanValue2;
            if (machine == null) {
                cleanValue2 = "";
            } else {
                cleanValue2 = machine.getName();
            }
            return new OperationView(
                    operation.getId(),
                    value,
                    GridWindow.clean(clean),
                    GridWindow.clean(cleanValue),
                    GridWindow.clean(process),
                    GridWindow.clean(operation.getOperatorName()),
                    GridWindow.clean(cleanValue2),
                    operation.getInputQuantity(),
                    operation.getOutputQuantity(),
                    operation.getWasteQuantity(),
                    GridWindow.clean(chamferLabel),
                    operation.getTotalOperationCost(),
                    operation.getFinishedAt()
            );
        }
    }
}
