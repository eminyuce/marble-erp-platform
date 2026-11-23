package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.ozerler.marble.model.enums.WorkshopWorkPurpose;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CutOrderView extends AuditView {
    private Long id;
    private String cutOrderNo;
    private String status;
    private String statusLabel;
    private String machineName;
    private String operatorName;
    private String notes;
    private LocalDate plannedStart;
    private WorkshopWorkPurpose purpose;
    private ProjectView project;
    private MachineRef machine;
    private List<ItemView> items;

    @Getter
    @Setter
    public static class ItemView {
        private String itemCode;
        private String edgeFinish;
        private String status;
        private String statusLabel;
        private BigDecimal thicknessCm;
        private BigDecimal widthCm;
        private BigDecimal lengthCm;
        private BigDecimal areaM2;
        private BigDecimal unitCost;
        private SlabRef sourceSlab;
    }
}
