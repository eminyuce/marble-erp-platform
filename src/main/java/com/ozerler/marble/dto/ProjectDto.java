package com.ozerler.marble.dto;

import com.ozerler.marble.model.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private String projectCode;
    private String name;
    private String customerName;
    private BigDecimal contractValue;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private BigDecimal costVariance;
    private BigDecimal costVariancePct;
    private LocalDate startDate;
    private LocalDate deliveryDate;
    private String status;
    private String statusLabel;
    private int locationCount;
    private BigDecimal totalPlannedAreaM2;
    private BigDecimal totalInstalledAreaM2;

    public static ProjectDto fromEntity(Project p) {
        BigDecimal variance = p.getActualCost().subtract(p.getEstimatedCost());
        BigDecimal varPct = p.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0 ?
                variance.divide(p.getEstimatedCost(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal plannedArea = p.getLocations() != null ?
                p.getLocations().stream()
                        .map(l -> l.getPlannedAreaM2() != null ? l.getPlannedAreaM2() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        BigDecimal installedArea = p.getLocations() != null ?
                p.getLocations().stream()
                        .map(l -> l.getInstalledAreaM2() != null ? l.getInstalledAreaM2() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        return ProjectDto.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .name(p.getName())
                .customerName(p.getCustomerName())
                .contractValue(p.getContractValue())
                .estimatedCost(p.getEstimatedCost())
                .actualCost(p.getActualCost())
                .costVariance(variance)
                .costVariancePct(varPct)
                .startDate(p.getStartDate())
                .deliveryDate(p.getDeliveryDate())
                .status(p.getStatus().name())
                .statusLabel(p.getStatus().getLabel())
                .locationCount(p.getLocations() != null ? p.getLocations().size() : 0)
                .totalPlannedAreaM2(plannedArea)
                .totalInstalledAreaM2(installedArea)
                .build();
    }
}
