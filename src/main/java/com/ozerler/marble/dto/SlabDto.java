package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Slab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @JsonProperty("surfaceFinishLabel")
    @JsonAlias("surface_finish_label")
    private String surfaceFinishLabel;

    @JsonProperty("qualityGrade")
    @JsonAlias("quality_grade")
    private String qualityGrade;

    @JsonProperty("qualityGradeLabel")
    @JsonAlias("quality_grade_label")
    private String qualityGradeLabel;

    @JsonProperty("glossLevel")
    @JsonAlias("gloss_level")
    private Integer glossLevel;

    @JsonProperty("costPerM2")
    @JsonAlias("cost_per_m2")
    private BigDecimal costPerM2;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusLabel")
    @JsonAlias("status_label")
    private String statusLabel;

    @JsonProperty("createdAt")
    @JsonAlias("created_at")
    private LocalDateTime createdAt;

    public static SlabDto fromEntity(Slab s) {
        if (s == null) {
            return null;
        }
        Long blockIdValue;
        if (s.getBlock() != null) {
            blockIdValue = s.getBlock().getId();
        } else {
            blockIdValue = null;
        }
        String blockCodeValue;
        if (s.getBlock() != null) {
            blockCodeValue = s.getBlock().getBlockCode();
        } else {
            blockCodeValue = null;
        }
        Long orderIdValue;
        if (s.getProductionOrder() != null) {
            orderIdValue = s.getProductionOrder().getId();
        } else {
            orderIdValue = null;
        }
        String orderNoValue;
        if (s.getProductionOrder() != null) {
            orderNoValue = s.getProductionOrder().getOrderNo();
        } else {
            orderNoValue = null;
        }
        Long palletIdValue;
        if (s.getPallet() != null) {
            palletIdValue = s.getPallet().getId();
        } else {
            palletIdValue = null;
        }
        String palletCodeValue;
        if (s.getPallet() != null) {
            palletCodeValue = s.getPallet().getPalletCode();
        } else {
            palletCodeValue = null;
        }
        String surfaceFinishValue;
        if (s.getSurfaceFinish() != null) {
            surfaceFinishValue = s.getSurfaceFinish().name();
        } else {
            surfaceFinishValue = null;
        }
        String surfaceFinishLabelValue;
        if (s.getSurfaceFinish() != null) {
            surfaceFinishLabelValue = s.getSurfaceFinish().getLabel();
        } else {
            surfaceFinishLabelValue = null;
        }
        String qualityGradeValue;
        if (s.getQualityGrade() != null) {
            qualityGradeValue = s.getQualityGrade().name();
        } else {
            qualityGradeValue = null;
        }
        String qualityGradeLabelValue;
        if (s.getQualityGrade() != null) {
            qualityGradeLabelValue = s.getQualityGrade().getLabel();
        } else {
            qualityGradeLabelValue = null;
        }
        String statusValue;
        if (s.getStatus() != null) {
            statusValue = s.getStatus().name();
        } else {
            statusValue = null;
        }
        String statusLabelValue;
        if (s.getStatus() != null) {
            statusLabelValue = s.getStatus().getLabel();
        } else {
            statusLabelValue = null;
        }
        return SlabDto.builder()
                .id(s.getId())
                .slabCode(s.getSlabCode())
                .blockId(blockIdValue)
                .blockCode(blockCodeValue)
                .orderId(orderIdValue)
                .orderNo(orderNoValue)
                .palletId(palletIdValue)
                .palletCode(palletCodeValue)
                .thicknessCm(s.getThicknessCm())
                .widthCm(s.getWidthCm())
                .lengthCm(s.getLengthCm())
                .surfaceAreaM2(s.getSurfaceAreaM2())
                .surfaceFinish(surfaceFinishValue)
                .surfaceFinishLabel(surfaceFinishLabelValue)
                .qualityGrade(qualityGradeValue)
                .qualityGradeLabel(qualityGradeLabelValue)
                .glossLevel(s.getGlossLevel())
                .costPerM2(s.getCostPerM2())
                .status(statusValue)
                .statusLabel(statusLabelValue)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
