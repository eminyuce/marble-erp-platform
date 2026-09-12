package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.ProductionOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing factory sawing and production orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @JsonProperty("order_no")
    @JsonAlias("orderNo")
    private String orderNo;

    @JsonProperty("block_id")
    @JsonAlias("blockId")
    private Long blockId;

    @JsonProperty("block_code")
    @JsonAlias("blockCode")
    private String blockCode;

    @JsonProperty("stone_type")
    @JsonAlias("stoneType")
    private String stoneType;

    @JsonProperty("machine_name")
    @JsonAlias("machineName")
    private String machineName;

    @JsonProperty("process_type")
    @JsonAlias("processType")
    private String processType;

    @JsonProperty("process_label")
    @JsonAlias("processLabel")
    private String processLabel;

    @JsonProperty("start_time")
    @JsonAlias("startTime")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    @JsonAlias("endTime")
    private LocalDateTime endTime;

    @JsonProperty("duration_hours")
    @JsonAlias("durationHours")
    private BigDecimal durationHours;

    @JsonProperty("electricity_kwh")
    @JsonAlias("electricityKwh")
    private BigDecimal electricityKwh;

    @JsonProperty("blade_wear_mm")
    @JsonAlias("bladeWearMm")
    private BigDecimal bladeWearMm;

    @JsonProperty("operator_name")
    @JsonAlias("operatorName")
    private String operatorName;

    @JsonProperty("status")
    @JsonAlias("status")
    private String status;

    @JsonProperty("slab_count")
    @JsonAlias("slabCount")
    private int slabCount;

    @JsonProperty("total_slab_area_m2")
    @JsonAlias("totalSlabAreaM2")
    private BigDecimal totalSlabAreaM2;

    @JsonProperty("notes")
    @JsonAlias("notes")
    private String notes;

    public static ProductionOrderDto fromEntity(ProductionOrder p) {
        return fromEntity(p, 0, BigDecimal.ZERO);
    }

    public static ProductionOrderDto fromEntity(ProductionOrder p, int slabCount, BigDecimal totalSlabAreaM2) {
        return ProductionOrderDto.builder()
                .id(p.getId())
                .orderNo(p.getOrderNo())
                .blockId(p.getBlock().getId())
                .blockCode(p.getBlock().getBlockCode())
                .stoneType(p.getBlock().getStoneType())
                .machineName(p.getMachineName())
                .processType(p.getProcessType().name())
                .processLabel(p.getProcessType().getLabel())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .durationHours(p.getDurationHours())
                .electricityKwh(p.getElectricityKwh())
                .bladeWearMm(p.getBladeWearMm())
                .operatorName(p.getOperatorName())
                .status(p.getStatus())
                .slabCount(slabCount)
                .totalSlabAreaM2(totalSlabAreaM2 != null ? totalSlabAreaM2 : BigDecimal.ZERO)
                .notes(p.getNotes())
                .build();
    }
}
