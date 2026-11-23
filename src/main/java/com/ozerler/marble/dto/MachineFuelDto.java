package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ozerler.marble.model.MachineFuelEntry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineFuelDto {
    private Long id;
    private LocalDate entryDate;
    private Long machineId;
    private String machineName;
    private String machineCode;
    private String businessUnit;
    private String businessUnitLabel;
    private BigDecimal litres;
    private BigDecimal pricePerLitre;
    private BigDecimal totalAmount;
    private BigDecimal workingHoursOrKm;
    private String receiptNo;
    private String issuedBy;
    private String receivedBy;
    private String notes;

    public static MachineFuelDto fromEntity(MachineFuelEntry entry) {
        if (entry == null) {
            return null;
        }
        Long machineIdValue;
        if (entry.getMachine() != null) {
            machineIdValue = entry.getMachine().getId();
        } else {
            machineIdValue = null;
        }
        String machineNameValue;
        if (entry.getMachine() != null) {
            machineNameValue = entry.getMachine().getName();
        } else {
            machineNameValue = "—";
        }
        String machineCodeValue;
        if (entry.getMachine() != null) {
            machineCodeValue = entry.getMachine().getCode();
        } else {
            machineCodeValue = "—";
        }
        String businessUnitValue;
        if (entry.getMachine() != null && entry.getMachine().getBusinessUnit() != null) {
            businessUnitValue = entry.getMachine().getBusinessUnit().name();
        } else {
            businessUnitValue = "";
        }
        String businessUnitLabelValue;
        if (entry.getMachine() != null && entry.getMachine().getBusinessUnit() != null) {
            businessUnitLabelValue = entry.getMachine().getBusinessUnit().getLabel();
        } else {
            businessUnitLabelValue = "";
        }
        return MachineFuelDto.builder()
                .id(entry.getId())
                .entryDate(entry.getEntryDate())
                .machineId(machineIdValue)
                .machineName(machineNameValue)
                .machineCode(machineCodeValue)
                .businessUnit(businessUnitValue)
                .businessUnitLabel(businessUnitLabelValue)
                .litres(entry.getLitres())
                .pricePerLitre(entry.getPricePerLitre())
                .totalAmount(entry.getTotalAmount())
                .workingHoursOrKm(entry.getWorkingHoursOrKm())
                .receiptNo(entry.getReceiptNo())
                .issuedBy(entry.getIssuedBy())
                .receivedBy(entry.getReceivedBy())
                .notes(entry.getNotes())
                .build();
    }
}
