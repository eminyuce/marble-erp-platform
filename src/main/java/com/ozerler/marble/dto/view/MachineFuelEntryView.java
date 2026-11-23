package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

/**
 * One fuel dispense row as the fuel screens show it.
 */
@Getter
@Setter
public class MachineFuelEntryView {
    private Long id;
    private MachineRef machine;
    private LocalDate entryDate;
    private BigDecimal litres;
    private BigDecimal pricePerLitre;
    private BigDecimal totalAmount;
    private BigDecimal workingHoursOrKm;
    private String receiptNo;
    private String issuedBy;
    private String receivedBy;
    private String notes;
}
