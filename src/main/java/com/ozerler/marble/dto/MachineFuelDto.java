package com.ozerler.marble.dto;

import com.ozerler.marble.model.MachineFuelEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        if (entry == null) return null;
        return MachineFuelDto.builder()
                .id(entry.getId())
                .entryDate(entry.getEntryDate())
                .machineId(entry.getMachine() != null ? entry.getMachine().getId() : null)
                .machineName(entry.getMachine() != null ? entry.getMachine().getName() : "—")
                .machineCode(entry.getMachine() != null ? entry.getMachine().getCode() : "—")
                .businessUnit(entry.getMachine() != null && entry.getMachine().getBusinessUnit() != null ? entry.getMachine().getBusinessUnit().name() : "")
                .businessUnitLabel(entry.getMachine() != null && entry.getMachine().getBusinessUnit() != null ? entry.getMachine().getBusinessUnit().getLabel() : "")
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
