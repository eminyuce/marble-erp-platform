package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.SalesOrder;
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
public class SalesOrderDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @JsonProperty("order_no")
    @JsonAlias("orderNo")
    private String orderNo;

    @JsonProperty("customer_name")
    @JsonAlias("customerName")
    private String customerName;

    @JsonProperty("customer_type")
    @JsonAlias("customerType")
    private String customerType;

    @JsonProperty("order_date")
    @JsonAlias("orderDate")
    private LocalDate orderDate;

    @JsonProperty("delivery_date")
    @JsonAlias("deliveryDate")
    private LocalDate deliveryDate;

    @JsonProperty("total_amount")
    @JsonAlias("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("paid_amount")
    @JsonAlias("paidAmount")
    private BigDecimal paidAmount;

    @JsonProperty("status")
    @JsonAlias("status")
    private String status;

    @JsonProperty("status_label")
    @JsonAlias("statusLabel")
    private String statusLabel;

    public static SalesOrderDto fromEntity(SalesOrder so) {
        return SalesOrderDto.builder()
                .id(so.getId())
                .orderNo(so.getOrderNo())
                .customerName(so.getCustomer().getCompanyName())
                .customerType(so.getCustomer().getCustomerType().getLabel())
                .orderDate(so.getOrderDate())
                .deliveryDate(so.getDeliveryDate())
                .totalAmount(so.getTotalAmount())
                .paidAmount(so.getPaidAmount())
                .status(so.getStatus().name())
                .statusLabel(so.getStatus().getLabel())
                .build();
    }
}
