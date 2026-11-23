package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.WorkshopProcessType;

import lombok.Getter;
import lombok.Setter;

/**
 * One workshop operation row on the cut order detail page.
 */
@Getter
@Setter
public class WorkshopOperationView {
    private Long id;
    private WorkshopProcessType processType;
    private OperationStatus status;
    private MachineRef machine;
    private String operatorName;
    private BigDecimal laborHours;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private BigDecimal inputAreaM2;
    private BigDecimal outputAreaM2;
    private BigDecimal wasteAreaM2;
    private BigDecimal extraExpense;
    private String notes;
}
