package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ozerler.marble.model.WorkshopMaterialReceipt;

public record WorkshopReceiptDto(
        String receiptNo,
        String sourceLabel,
        String supplierName,
        String stoneType,
        BigDecimal quantity,
        BigDecimal areaM2,
        BigDecimal purchaseCost,
        String receivedAt,
        String notes
) {
    public static WorkshopReceiptDto from(WorkshopMaterialReceipt receipt) {
        String stoneType;
        if (receipt.getMaterialLot() != null) {
            stoneType = receipt.getMaterialLot().getStoneType();
        } else {
            stoneType = "";
        }
        String supplierName;
        if (receipt.getSupplier() != null) {
            supplierName = receipt.getSupplier().getCompanyName();
        } else {
            supplierName = "";
        }
        String sourceLabel;
        if (receipt.getSource() != null) {
            sourceLabel = receipt.getSource().getLabel();
        } else {
            sourceLabel = "";
        }
        LocalDate receivedAt = receipt.getReceivedAt();
        String value;
        if (receivedAt != null) {
            value = receivedAt.toString();
        } else {
            value = "";
        }
        return new WorkshopReceiptDto(
                receipt.getReceiptNo(),
                sourceLabel,
                supplierName,
                stoneType,
                receipt.getQuantity(),
                receipt.getAreaM2(),
                receipt.getPurchaseCost(),
                value,
                receipt.getNotes()
        );
    }
}
