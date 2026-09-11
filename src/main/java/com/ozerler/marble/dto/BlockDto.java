package com.ozerler.marble.dto;

import com.ozerler.marble.model.Block;
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
public class BlockDto {
    private Long id;
    private Long quarryId;
    private String quarryName;
    private String blockCode;
    private LocalDate extractionDate;
    private Integer widthCm;
    private Integer lengthCm;
    private Integer heightCm;
    private BigDecimal volumeM3;
    private BigDecimal theoreticalWeightKg;
    private BigDecimal actualWeightKg;
    private BigDecimal weightDeviationPct;
    private String stoneType;
    private String colorTone;
    private String qualityGrade;
    private Integer crackLevel;
    private String status;
    private String statusLabel;
    private BigDecimal extractionCost;
    private BigDecimal transportCost;
    private BigDecimal totalCost;
    private String notes;
    private String photoUrls;

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
                .qualityGrade(b.getQualityGrade().name())
                .crackLevel(b.getCrackLevel())
                .status(b.getStatus().name())
                .statusLabel(b.getStatus().getLabel())
                .extractionCost(b.getExtractionCost())
                .transportCost(b.getTransportCost())
                .totalCost(b.getTotalCost())
                .notes(b.getNotes())
                .photoUrls(b.getPhotoUrls())
                .build();
    }
}
