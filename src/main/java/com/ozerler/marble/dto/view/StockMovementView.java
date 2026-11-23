package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.TargetDestination;
import lombok.Getter;
import lombok.Setter;

/**
 * One stock movement row for the quarry dispatch report.
 */
@Getter
@Setter
public class StockMovementView {
    private Long id;
    private LocalDateTime movementDate;
    private BusinessUnit targetDepartment;
    private TargetDestination targetDestination;
    private BlockRef block;
    private String itemDescription;
    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal tonnage;
    private PartyView customer;
    private InvoiceRef invoice;
    private String notes;
}
