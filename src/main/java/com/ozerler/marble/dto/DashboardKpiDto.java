package com.ozerler.marble.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strongly typed DTO transferring executive cockpit KPIs from DashboardService to presentation layer.
 */
@Getter
@Builder
public class DashboardKpiDto {
    private final BigDecimal totalSlabArea;
    private final BigDecimal availableSlabArea;
    private final BigDecimal reservedSlabArea;
    private final long factoryBlockCount;
    private final Double factoryBlockWeightTon;
    private final long activeProjectsCount;
    private final BigDecimal totalScrapImpact;
    private final List<ScrapSummaryDto> scrapSummary;
    private final long totalUsers;
    private final List<CostDistributionDto> costDistribution;

    // Integrated Operations KPIs
    private final long quarryBlockCount;
    private final long factoryBlockUncutCount;
    private final long factorySizedItemCount;
    private final BigDecimal factorySizedItemAreaM2;
    private final BigDecimal totalEstimatedTonnage;
    private final BigDecimal totalActualTonnage;
    private final long openWorkOrdersCount;
    private final long inProductionWorkOrdersCount;
    private final long completedWorkOrdersCount;
    private final BigDecimal monthlyPurchasesTotal;
    private final BigDecimal monthlySalesTotal;
    private final BigDecimal monthlyCollectionsTotal;
    private final long approachingChecksCount;
    private final BigDecimal approachingChecksTotal;
}
