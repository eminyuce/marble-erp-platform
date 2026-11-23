package com.ozerler.marble.dto.view;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class WorkOrderView {
    private Long id;
    private String orderNo;
    private String orderNumber;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private BusinessUnit department;
    private String responsiblePerson;
    private String stoneType;
    private String colorQuality;
    private BigDecimal thicknessCm;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
    private BigDecimal plannedQuantity;
    private String quantityUnit;
    private String surfaceOperation;
    private String edgeOperation;
    private BigDecimal usedQuantity;
    private String notes;
    private String customerNameForView;
    private OperationWorkOrderStatus status;
    private BlockRef sourceBlock;
    private SlabRef sourceSlab;
    private StockRef sourceStockItem;
    private List<ItemView> items;
    private List<HistoryView> statusHistory;

    public List<HistoryView> getStatusHistories() {
        return statusHistory;
    }

    @Getter
    @Setter
    public static class ItemView {
        private BigDecimal thicknessCm;
        private BigDecimal widthCm;
        private BigDecimal lengthCm;
        private Integer pieceCount;
        private BigDecimal calculatedM2;
        private BigDecimal calculatedMt;
        private String surfaceOperation;
        private String edgeOperation;
        private String surfaceUnit;
        private String notes;
    }

    @Getter
    @Setter
    public static class HistoryView {
        private OperationWorkOrderStatus newStatus;
        private String notes;
        private LocalDateTime changedAt;
    }
}
