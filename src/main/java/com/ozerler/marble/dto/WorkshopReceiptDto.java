package com.ozerler.marble.dto;

import com.ozerler.marble.model.WorkshopMaterialReceipt;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        String stoneType = receipt.getMaterialLot() != null ? receipt.getMaterialLot().getStoneType() : "";
        String supplierName = receipt.getSupplier() != null ? receipt.getSupplier().getCompanyName() : "";
        String sourceLabel = receipt.getSource() != null ? receipt.getSource().getLabel() : "";
        LocalDate receivedAt = receipt.getReceivedAt();
        return new WorkshopReceiptDto(
                receipt.getReceiptNo(),
                sourceLabel,
                supplierName,
                stoneType,
                receipt.getQuantity(),
                receipt.getAreaM2(),
                receipt.getPurchaseCost(),
                receivedAt != null ? receivedAt.toString() : "",
                receipt.getNotes()
        );
    }
}
