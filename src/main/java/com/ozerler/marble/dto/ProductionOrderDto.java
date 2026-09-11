package com.ozerler.marble.dto;

import com.ozerler.marble.model.ProductionOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderDto {
    private Long id;
    private String orderNo;
    private Long blockId;
    private String blockCode;
    private String stoneType;
    private String machineName;
    private String processType;
    private String processLabel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationHours;
    private BigDecimal electricityKwh;
    private BigDecimal bladeWearMm;
    private String operatorName;
    private String status;
    private int slabCount;
    private BigDecimal totalSlabAreaM2;
    private String notes;

    public static ProductionOrderDto fromEntity(ProductionOrder p) {
        BigDecimal totalArea = p.getSlabs() != null ?
                p.getSlabs().stream()
                        .map(s -> s.getSurfaceAreaM2() != null ? s.getSurfaceAreaM2() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

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
                .slabCount(p.getSlabs() != null ? p.getSlabs().size() : 0)
                .totalSlabAreaM2(totalArea)
                .notes(p.getNotes())
                .build();
    }
}
