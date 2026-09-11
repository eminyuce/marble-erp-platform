package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.PurchaseOrder;
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
public class PurchaseOrderDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @JsonProperty("po_number")
    @JsonAlias("poNumber")
    private String poNumber;

    @JsonProperty("supplier_name")
    @JsonAlias("supplierName")
    private String supplierName;

    @JsonProperty("supplier_type")
    @JsonAlias("supplierType")
    private String supplierType;

    @JsonProperty("project_name")
    @JsonAlias("projectName")
    private String projectName;

    @JsonProperty("order_date")
    @JsonAlias("orderDate")
    private LocalDate orderDate;

    @JsonProperty("expected_delivery")
    @JsonAlias("expectedDelivery")
    private LocalDate expectedDelivery;

    @JsonProperty("total_amount")
    @JsonAlias("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("status")
    @JsonAlias("status")
    private String status;

    @JsonProperty("status_label")
    @JsonAlias("statusLabel")
    private String statusLabel;

    public static PurchaseOrderDto fromEntity(PurchaseOrder po) {
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierName(po.getSupplier().getCompanyName())
                .supplierType(po.getSupplier().getSupplierType().getLabel())
                .projectName(po.getProject() != null ? po.getProject().getName() : "—")
                .orderDate(po.getOrderDate())
                .expectedDelivery(po.getExpectedDelivery())
                .totalAmount(po.getTotalAmount())
                .status(po.getStatus().name())
                .statusLabel(po.getStatus().getLabel())
                .build();
    }
}
