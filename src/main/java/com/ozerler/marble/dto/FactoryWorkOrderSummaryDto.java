package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.enums.FactoryWorkOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * View/API projection for factory work orders that never exposes a lazy Block proxy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryWorkOrderSummaryDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("orderNo")
    @JsonAlias("order_no")
    private String orderNo;

    @JsonProperty("blockCode")
    @JsonAlias("block_code")
    private String blockCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusLabel")
    @JsonAlias("status_label")
    private String statusLabel;

    public static FactoryWorkOrderSummaryDto fromEntity(FactoryWorkOrder workOrder) {
        Block block = workOrder.getBlock();
        FactoryWorkOrderStatus status = workOrder.getStatus();
        return FactoryWorkOrderSummaryDto.builder()
                .id(workOrder.getId())
                .orderNo(workOrder.getOrderNo())
                .blockCode(block != null ? block.getBlockCode() : "")
                .status(status != null ? status.name() : "")
                .statusLabel(status != null ? status.getLabel() : "")
                .build();
    }
}
