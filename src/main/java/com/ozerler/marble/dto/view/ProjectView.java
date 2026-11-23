package com.ozerler.marble.dto.view;

import com.ozerler.marble.model.enums.ProjectStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectView extends AuditView {
    private Long id;
    private String name;
    private String projectCode;
    private String customerName;
    private String notes;
    private BigDecimal contractValue;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private LocalDate startDate;
    private LocalDate deliveryDate;
    private ProjectStatus status;
    private PartyView customer;
}
