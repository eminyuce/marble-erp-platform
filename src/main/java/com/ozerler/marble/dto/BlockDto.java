package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.enums.BlockStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @JsonProperty("quarry_section")
    @JsonAlias("quarrySection")
    private String quarrySection;

    @JsonProperty("extraction_date")
    @JsonAlias("extractionDate")
    private LocalDate extractionDate;

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private LocalDateTime createdAt;

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

    @JsonProperty("location_type")
    @JsonAlias("locationType")
    private String locationType;

    @JsonProperty("sold_customer_name")
    @JsonAlias("soldCustomerName")
    private String soldCustomerName;

    @JsonProperty("assigned_customer_id")
    @JsonAlias("assignedCustomerId")
    private Long assignedCustomerId;

    @JsonProperty("assigned_customer_name")
    @JsonAlias("assignedCustomerName")
    private String assignedCustomerName;

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

    @JsonProperty("canDelete")
    @JsonAlias("canDelete")
    private boolean canDelete;

    @JsonProperty("surface_area_m2")
    @JsonAlias("surfaceAreaM2")
    private BigDecimal surfaceAreaM2;

    @JsonProperty("calculated_extraction_cost")
    @JsonAlias("calculatedExtractionCost")
    private BigDecimal calculatedExtractionCost;

    @JsonProperty("market_value")
    @JsonAlias("marketValue")
    private BigDecimal marketValue;

    @JsonProperty("sale_price")
    @JsonAlias("salePrice")
    private BigDecimal salePrice;

    @JsonProperty("unit_market_value_per_ton")
    @JsonAlias("unitMarketValuePerTon")
    private BigDecimal unitMarketValuePerTon;

    public static BlockDto fromEntity(Block b) {
        boolean deletable = b.getStatus() == BlockStatus.PRODUCED
                && b.getSoldCustomer() == null
                && (b.getTransportCost() == null || b.getTransportCost().compareTo(BigDecimal.ZERO) <= 0);
        return fromEntity(b, deletable);
    }

    public static BlockDto fromEntity(Block b, boolean canDelete) {
        return fromEntity(b, canDelete, null, null, null, null);
    }

    public static BlockDto fromEntity(Block b, boolean canDelete, BigDecimal calculatedExtractionCost,
                                      BigDecimal totalCost, BigDecimal surfaceAreaM2, BigDecimal marketValue) {
        Long quarryIdValue;
        if (b.getQuarry() != null) {
            quarryIdValue = b.getQuarry().getId();
        } else {
            quarryIdValue = null;
        }
        String quarryNameValue;
        if (b.getQuarry() != null) {
            quarryNameValue = b.getQuarry().getName();
        } else {
            quarryNameValue = "";
        }
        String blockCodeValue;
        if (b.getBlockCode() != null) {
            blockCodeValue = b.getBlockCode().toUpperCase(Locale.ROOT);
        } else {
            blockCodeValue = "";
        }
        LocalDateTime createdAtValue;
        if (b.getCreatedDate() != null) {
            createdAtValue = b.getCreatedDate();
        } else {
            if (b.getExtractionDate() != null) {
                createdAtValue = b.getExtractionDate().atStartOfDay();
            } else {
                createdAtValue = null;
            }
        }
        String qualityGradeValue;
        if (b.getQualityGrade() != null) {
            qualityGradeValue = b.getQualityGrade().name();
        } else {
            qualityGradeValue = "";
        }
        String qualityGradeLabelValue;
        if (b.getQualityGrade() != null) {
            qualityGradeLabelValue = b.getQualityGrade().getLabel();
        } else {
            qualityGradeLabelValue = "";
        }
        String statusValue;
        if (b.getStatus() != null) {
            statusValue = b.getStatus().name();
        } else {
            statusValue = "";
        }
        String statusLabelValue;
        if (b.getStatus() != null) {
            statusLabelValue = b.getStatus().getLabel();
        } else {
            statusLabelValue = "";
        }
        BigDecimal extractionCostValue;
        if (calculatedExtractionCost != null) {
            extractionCostValue = calculatedExtractionCost;
        } else {
            extractionCostValue = b.getExtractionCost();
        }
        BigDecimal totalCostValue;
        if (totalCost != null) {
            totalCostValue = totalCost;
        } else {
            totalCostValue = b.getTotalCost();
        }
        String locationNameValue;
        if (b.getCurrentLocation() != null) {
            locationNameValue = b.getCurrentLocation().getName();
        } else {
            locationNameValue = "";
        }
        String locationTypeValue;
        if (b.getCurrentLocation() != null && b.getCurrentLocation().getLocationType() != null) {
            locationTypeValue = b.getCurrentLocation().getLocationType().name();
        } else {
            locationTypeValue = "";
        }
        String soldCustomerNameValue;
        if (b.getSoldCustomer() != null) {
            soldCustomerNameValue = b.getSoldCustomer().getCompanyName();
        } else {
            soldCustomerNameValue = "";
        }
        Long assignedCustomerIdValue;
        if (b.getAssignedCustomer() != null) {
            assignedCustomerIdValue = b.getAssignedCustomer().getId();
        } else {
            assignedCustomerIdValue = null;
        }
        String assignedCustomerNameValue;
        if (b.getAssignedCustomer() != null) {
            assignedCustomerNameValue = b.getAssignedCustomer().getCompanyName();
        } else {
            assignedCustomerNameValue = "";
        }
        return BlockDto.builder()
                .id(b.getId())
                .quarryId(quarryIdValue)
                .quarryName(quarryNameValue)
                .blockCode(blockCodeValue)
                .quarrySection(b.getQuarrySection())
                .extractionDate(b.getExtractionDate())
                .createdAt(createdAtValue)
                .widthCm(b.getWidthCm())
                .lengthCm(b.getLengthCm())
                .heightCm(b.getHeightCm())
                .volumeM3(b.getVolumeM3())
                .theoreticalWeightKg(b.getTheoreticalWeightKg())
                .actualWeightKg(b.getActualWeightKg())
                .weightDeviationPct(b.getWeightDeviationPct())
                .stoneType(b.getStoneType())
                .colorTone(b.getColorTone())
                .qualityGrade(qualityGradeValue)
                .qualityGradeLabel(qualityGradeLabelValue)
                .crackLevel(b.getCrackLevel())
                .status(statusValue)
                .statusLabel(statusLabelValue)
                .extractionCost(extractionCostValue)
                .transportCost(b.getTransportCost())
                .totalCost(totalCostValue)
                .calculatedExtractionCost(calculatedExtractionCost)
                .surfaceAreaM2(surfaceAreaM2)
                .marketValue(marketValue)
                .salePrice(b.getSalePrice())
                .unitMarketValuePerTon(b.getUnitMarketValuePerTon())
                .notes(b.getNotes())
                .photoUrls(b.getPhotoUrls())
                .locationName(locationNameValue)
                .locationType(locationTypeValue)
                .soldCustomerName(soldCustomerNameValue)
                .assignedCustomerId(assignedCustomerIdValue)
                .assignedCustomerName(assignedCustomerNameValue)
                .approximateTonnage(b.getApproximateTonnage())
                .actualTonnage(b.getActualTonnage())
                .canonicalStatus(b.getCanonicalStatus().name())
                .weightDeviationWarning(b.isWeightDeviationWarning())
                .canDelete(canDelete)
                .build();
    }
}
