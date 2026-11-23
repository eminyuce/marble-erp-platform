package com.ozerler.marble.dto;

import java.time.LocalDate;
import java.util.Objects;

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
        String blockCodeValue;
        if (block != null) {
            blockCodeValue = block.getBlockCode();
        } else {
            blockCodeValue = "";
        }
        String statusValue;
        if (status != null) {
            statusValue = status.name();
        } else {
            statusValue = "";
        }
        String statusLabelValue;
        if (status != null) {
            statusLabelValue = status.getLabel();
        } else {
            statusLabelValue = "";
        }
        String stoneTypeValue;
        if (block != null && block.getStoneType() != null) {
            stoneTypeValue = block.getStoneType();
        } else {
            stoneTypeValue = "";
        }
        String acceptedAtValue;
        if (acceptedAt != null) {
            acceptedAtValue = acceptedAt.toString();
        } else {
            acceptedAtValue = "";
        }
        String machineNameValue;
        if (machine != null) {
            machineNameValue = machine.getName();
        } else {
            machineNameValue = "";
        }
        return FactoryWorkOrderSummaryDto.builder()
                .id(workOrder.getId())
                .orderNo(workOrder.getOrderNo())
                .blockCode(blockCodeValue)
                .status(statusValue)
                .statusLabel(statusLabelValue)
                .stoneType(stoneTypeValue)
                .acceptedAt(acceptedAtValue)
                .machineName(machineNameValue)
                .responsibleName(Objects.requireNonNullElse(workOrder.getResponsibleName(), ""))
                .build();
    }
}
