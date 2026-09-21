package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Data Transfer Object representing architecture and installation projects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @JsonProperty("project_code")
    @JsonAlias("projectCode")
    private String projectCode;

    @JsonProperty("name")
    @JsonAlias("name")
    private String name;

    @JsonProperty("customer_name")
    @JsonAlias("customerName")
    private String customerName;

    @JsonProperty("contract_value")
    @JsonAlias("contractValue")
    private BigDecimal contractValue;

    @JsonProperty("estimated_cost")
    @JsonAlias("estimatedCost")
    private BigDecimal estimatedCost;

    @JsonProperty("actual_cost")
    @JsonAlias("actualCost")
    private BigDecimal actualCost;

    @JsonProperty("cost_variance")
    @JsonAlias("costVariance")
    private BigDecimal costVariance;

    @JsonProperty("cost_variance_pct")
    @JsonAlias("costVariancePct")
    private BigDecimal costVariancePct;

    @JsonProperty("start_date")
    @JsonAlias("startDate")
    private LocalDate startDate;

    @JsonProperty("delivery_date")
    @JsonAlias("deliveryDate")
    private LocalDate deliveryDate;

    @JsonProperty("status")
    @JsonAlias("status")
    private String status;

    @JsonProperty("status_label")
    @JsonAlias("statusLabel")
    private String statusLabel;

    @JsonProperty("location_count")
    @JsonAlias("locationCount")
    private int locationCount;

    @JsonProperty("total_planned_area_m2")
    @JsonAlias("totalPlannedAreaM2")
    private BigDecimal totalPlannedAreaM2;

    @JsonProperty("total_installed_area_m2")
    @JsonAlias("totalInstalledAreaM2")
    private BigDecimal totalInstalledAreaM2;

    public static ProjectDto fromEntity(Project p) {
        BigDecimal variance = p.getActualCost().subtract(p.getEstimatedCost());
        BigDecimal varPct = p.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0 ?
                variance.divide(p.getEstimatedCost(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal plannedArea = p.getLocations() != null ?
                p.getLocations().stream()
                        .map(l -> l.getPlannedAreaM2() != null ? l.getPlannedAreaM2() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b))
                : BigDecimal.ZERO;

        BigDecimal installedArea = p.getLocations() != null ?
                p.getLocations().stream()
                        .map(l -> l.getInstalledAreaM2() != null ? l.getInstalledAreaM2() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b))
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
