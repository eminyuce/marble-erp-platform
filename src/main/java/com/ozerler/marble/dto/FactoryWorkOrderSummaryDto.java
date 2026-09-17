package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.enums.FactoryWorkOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    @JsonProperty("stoneType")
    @JsonAlias("stone_type")
    private String stoneType;

    @JsonProperty("acceptedAt")
    @JsonAlias("accepted_at")
    private String acceptedAt;

    @JsonProperty("machineName")
    @JsonAlias("machine_name")
    private String machineName;

    @JsonProperty("responsibleName")
    @JsonAlias("responsible_name")
    private String responsibleName;

    public static FactoryWorkOrderSummaryDto fromEntity(FactoryWorkOrder workOrder) {
        Block block = workOrder.getBlock();
        Machine machine = workOrder.getAssignedMachine();
        FactoryWorkOrderStatus status = workOrder.getStatus();
        LocalDate acceptedAt = workOrder.getAcceptedAt();
        return FactoryWorkOrderSummaryDto.builder()
                .id(workOrder.getId())
                .orderNo(workOrder.getOrderNo())
                .blockCode(block != null ? block.getBlockCode() : "")
                .status(status != null ? status.name() : "")
                .statusLabel(status != null ? status.getLabel() : "")
                .stoneType(block != null && block.getStoneType() != null ? block.getStoneType() : "")
                .acceptedAt(acceptedAt != null ? acceptedAt.toString() : "")
                .machineName(machine != null ? machine.getName() : "")
                .responsibleName(workOrder.getResponsibleName() != null ? workOrder.getResponsibleName() : "")
                .build();
    }
}
