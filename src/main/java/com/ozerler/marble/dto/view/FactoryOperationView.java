package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ozerler.marble.model.enums.ChamferStatus;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * Read model of one factory surface operation for the polish edit screen.
 */
@Getter
@Setter
public class FactoryOperationView {
    private Long id;
    private FactoryProcessType processType;
    private ChamferStatus chamferStatus;
    private OperationStatus status;
    private String operatorName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private BigDecimal inputQuantity;
    private BigDecimal outputQuantity;
    private BigDecimal wasteQuantity;
    private BigDecimal laborCost;
    private BigDecimal electricityCost;
    private BigDecimal consumableCost;
    private BigDecimal totalOperationCost;
    private String notes;
    private MachineRef machine;
    private WorkOrderRef workOrder;

    /**
     * Factory work order reference with its block.
     */
    @Getter
    @Setter
    public static class WorkOrderRef {
        private Long id;
        private String orderNo;
        private BlockView block;
    }
}
