package com.ozerler.marble.service;

import com.ozerler.marble.dto.CostDistributionDto;
import com.ozerler.marble.dto.DashboardKpiDto;
import com.ozerler.marble.dto.ScrapSummaryDto;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service orchestrating General Management Cockpit (Genel Müdür Tek Ekran Canlı Kokpiti - BRD Section 9.1)
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final ProjectRepository projectRepository;
    private final ScrapLogRepository scrapLogRepository;
    private final UserRepository userRepository;
    private final CostTransactionRepository costTransactionRepository;

    @Transactional(readOnly = true)
    public DashboardKpiDto getDashboardKpis() {
        // KPI 1: Physical Stock
        BigDecimal totalSlabArea = slabRepository.getTotalInventoryAreaM2();
        BigDecimal availableSlabArea = slabRepository.getTotalAreaByStatus(SlabStatus.AVAILABLE);
        BigDecimal reservedSlabArea = slabRepository.getTotalAreaByStatus(SlabStatus.RESERVED);
        long factoryBlockCount = blockRepository.countByStatus(BlockStatus.FACTORY_STOCK);
        Double factoryBlockWeight = blockRepository.getTotalFactoryStockWeightKg();
        Double factoryBlockWeightTon = factoryBlockWeight != null ? factoryBlockWeight / 1000.0 : 0.0;

        // KPI 2: Active Projects
        long activeProjectsCount = projectRepository.countByStatus(ProjectStatus.ACTIVE);

        // KPI 3: Scrap & Waste Impact
        BigDecimal totalScrapImpact = scrapLogRepository.getTotalScrapCostImpact();
        List<ScrapSummaryDto> scrapSummary = scrapLogRepository.getScrapSummaryByReason().stream()
                .map(ScrapSummaryDto::fromRow)
                .toList();

        // KPI 4: Users & System metrics
        long totalUsers = userRepository.countByDeletedFalse();

        // Cost distribution
        List<CostDistributionDto> costDistribution = costTransactionRepository.getCostDistributionByCenter().stream()
                .map(CostDistributionDto::fromRow)
                .toList();

        return DashboardKpiDto.builder()
                .totalSlabArea(totalSlabArea)
                .availableSlabArea(availableSlabArea)
                .reservedSlabArea(reservedSlabArea)
                .factoryBlockCount(factoryBlockCount)
                .factoryBlockWeightTon(factoryBlockWeightTon)
                .activeProjectsCount(activeProjectsCount)
                .totalScrapImpact(totalScrapImpact)
                .scrapSummary(scrapSummary)
                .totalUsers(totalUsers)
                .costDistribution(costDistribution)
                .build();
    }
}
