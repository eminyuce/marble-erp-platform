package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Block;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object representing marble block details and metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @JsonProperty("quarry_id")
    @JsonAlias("quarryId")
    private Long quarryId;

    @JsonProperty("quarry_name")
    @JsonAlias("quarryName")
    private String quarryName;

    @JsonProperty("block_code")
    @JsonAlias("blockCode")
    private String blockCode;

    @JsonProperty("extraction_date")
    @JsonAlias("extractionDate")
    private LocalDate extractionDate;

    @JsonProperty("width_cm")
    @JsonAlias("widthCm")
    private Integer widthCm;

    @JsonProperty("length_cm")
    @JsonAlias("lengthCm")
    private Integer lengthCm;

    @JsonProperty("height_cm")
    @JsonAlias("heightCm")
    private Integer heightCm;

    @JsonProperty("volume_m3")
    @JsonAlias("volumeM3")
    private BigDecimal volumeM3;

    @JsonProperty("theoretical_weight_kg")
    @JsonAlias("theoreticalWeightKg")
    private BigDecimal theoreticalWeightKg;

    @JsonProperty("actual_weight_kg")
    @JsonAlias("actualWeightKg")
    private BigDecimal actualWeightKg;

    @JsonProperty("weight_deviation_pct")
    @JsonAlias("weightDeviationPct")
    private BigDecimal weightDeviationPct;

    @JsonProperty("stone_type")
    @JsonAlias("stoneType")
    private String stoneType;

    @JsonProperty("color_tone")
    @JsonAlias("colorTone")
    private String colorTone;

    @JsonProperty("quality_grade")
    @JsonAlias("qualityGrade")
    private String qualityGrade;

    @JsonProperty("quality_grade_label")
    @JsonAlias("qualityGradeLabel")
    private String qualityGradeLabel;

    @JsonProperty("crack_level")
    @JsonAlias("crackLevel")
    private Integer crackLevel;

    @JsonProperty("status")
    @JsonAlias("status")
    private String status;

    @JsonProperty("status_label")
    @JsonAlias("statusLabel")
    private String statusLabel;

    @JsonProperty("extraction_cost")
    @JsonAlias("extractionCost")
    private BigDecimal extractionCost;

    @JsonProperty("transport_cost")
    @JsonAlias("transportCost")
    private BigDecimal transportCost;

    @JsonProperty("total_cost")
    @JsonAlias("totalCost")
    private BigDecimal totalCost;

    @JsonProperty("notes")
    @JsonAlias("notes")
    private String notes;

    @JsonProperty("photo_urls")
    @JsonAlias("photoUrls")
    private String photoUrls;

    @JsonProperty("location_name")
    @JsonAlias("locationName")
    private String locationName;

    @JsonProperty("approximate_tonnage")
    @JsonAlias("approximateTonnage")
    private BigDecimal approximateTonnage;

    @JsonProperty("actual_tonnage")
    @JsonAlias("actualTonnage")
    private BigDecimal actualTonnage;

    @JsonProperty("canonical_status")
    @JsonAlias("canonicalStatus")
    private String canonicalStatus;

    @JsonProperty("weight_deviation_warning")
    @JsonAlias("weightDeviationWarning")
    private boolean weightDeviationWarning;

    public static BlockDto fromEntity(Block b) {
        return BlockDto.builder()
                .id(b.getId())
                .quarryId(b.getQuarry().getId())
                .quarryName(b.getQuarry().getName())
                .blockCode(b.getBlockCode())
                .extractionDate(b.getExtractionDate())
                .widthCm(b.getWidthCm())
                .lengthCm(b.getLengthCm())
                .heightCm(b.getHeightCm())
                .volumeM3(b.getVolumeM3())
                .theoreticalWeightKg(b.getTheoreticalWeightKg())
                .actualWeightKg(b.getActualWeightKg())
                .weightDeviationPct(b.getWeightDeviationPct())
                .stoneType(b.getStoneType())
                .colorTone(b.getColorTone())
                .qualityGrade(b.getQualityGrade() != null ? b.getQualityGrade().name() : "")
                .qualityGradeLabel(b.getQualityGrade() != null ? b.getQualityGrade().getLabel() : "")
                .crackLevel(b.getCrackLevel())
                .status(b.getStatus().name())
                .statusLabel(b.getStatus().getLabel())
                .extractionCost(b.getExtractionCost())
                .transportCost(b.getTransportCost())
                .totalCost(b.getTotalCost())
                .notes(b.getNotes())
                .photoUrls(b.getPhotoUrls())
                .locationName(b.getCurrentLocation() != null ? b.getCurrentLocation().getName() : "")
                .approximateTonnage(b.getApproximateTonnage())
                .actualTonnage(b.getActualTonnage())
                .canonicalStatus(b.getCanonicalStatus().name())
                .weightDeviationWarning(b.isWeightDeviationWarning())
                .build();
    }
}
