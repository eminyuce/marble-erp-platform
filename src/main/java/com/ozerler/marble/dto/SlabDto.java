package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Slab;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing slab inventory details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlabDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("slabCode")
    @JsonAlias("slab_code")
    private String slabCode;

    @JsonProperty("blockId")
    @JsonAlias("block_id")
    private Long blockId;

    @JsonProperty("blockCode")
    @JsonAlias("block_code")
    private String blockCode;

    @JsonProperty("orderId")
    @JsonAlias("order_id")
    private Long orderId;

    @JsonProperty("orderNo")
    @JsonAlias("order_no")
    private String orderNo;

    @JsonProperty("palletId")
    @JsonAlias("pallet_id")
    private Long palletId;

    @JsonProperty("palletCode")
    @JsonAlias("pallet_code")
    private String palletCode;

    @JsonProperty("thicknessCm")
    @JsonAlias("thickness_cm")
    private BigDecimal thicknessCm;

    @JsonProperty("widthCm")
    @JsonAlias("width_cm")
    private BigDecimal widthCm;

    @JsonProperty("lengthCm")
    @JsonAlias("length_cm")
    private BigDecimal lengthCm;

    @JsonProperty("surfaceAreaM2")
    @JsonAlias("surface_area_m2")
    private BigDecimal surfaceAreaM2;

    @JsonProperty("surfaceFinish")
    @JsonAlias("surface_finish")
    private String surfaceFinish;

    @JsonProperty("qualityGrade")
    @JsonAlias("quality_grade")
    private String qualityGrade;

    @JsonProperty("glossLevel")
    @JsonAlias("gloss_level")
    private Integer glossLevel;

    @JsonProperty("costPerM2")
    @JsonAlias("cost_per_m2")
    private BigDecimal costPerM2;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdAt")
    @JsonAlias("created_at")
    private LocalDateTime createdAt;

    public static SlabDto fromEntity(Slab s) {
        if (s == null) {
            return null;
        }
        return SlabDto.builder()
                .id(s.getId())
                .slabCode(s.getSlabCode())
                .blockId(s.getBlock() != null ? s.getBlock().getId() : null)
                .blockCode(s.getBlock() != null ? s.getBlock().getBlockCode() : null)
                .orderId(s.getProductionOrder() != null ? s.getProductionOrder().getId() : null)
                .orderNo(s.getProductionOrder() != null ? s.getProductionOrder().getOrderNo() : null)
                .palletId(s.getPallet() != null ? s.getPallet().getId() : null)
                .palletCode(s.getPallet() != null ? s.getPallet().getPalletCode() : null)
                .thicknessCm(s.getThicknessCm())
                .widthCm(s.getWidthCm())
                .lengthCm(s.getLengthCm())
                .surfaceAreaM2(s.getSurfaceAreaM2())
                .surfaceFinish(s.getSurfaceFinish() != null ? s.getSurfaceFinish().name() : null)
                .qualityGrade(s.getQualityGrade() != null ? s.getQualityGrade().name() : null)
                .glossLevel(s.getGlossLevel())
                .costPerM2(s.getCostPerM2())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
