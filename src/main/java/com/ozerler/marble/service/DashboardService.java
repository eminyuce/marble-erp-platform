package com.ozerler.marble.service;

import com.ozerler.marble.dto.CostDistributionDto;
import com.ozerler.marble.dto.DashboardKpiDto;
import com.ozerler.marble.dto.ScrapSummaryDto;
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
    private final StockItemRepository stockItemRepository;
    private final OperationWorkOrderRepository workOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final CollectionRecordRepository collectionRepository;
    private final CheckRecordRepository checkRepository;

    @Transactional(readOnly = true)
    public DashboardKpiDto getDashboardKpis() {
        // KPI 1: Physical Stock
        BigDecimal totalSlabArea = slabRepository.getTotalInventoryAreaM2();
        BigDecimal availableSlabArea = slabRepository.getTotalAreaByStatus(SlabStatus.AVAILABLE);
        BigDecimal reservedSlabArea = slabRepository.getTotalAreaByStatus(SlabStatus.RESERVED);
        long factoryBlockCount = blockRepository.getCountFactoryStock();
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

        // Operational & Commerce KPIs
        long quarryBlockCount = blockRepository.countQuarryBlocks();
        long factoryBlockUncutCount = blockRepository.countFactoryUncutBlocks();
        long factorySizedItemCount = stockItemRepository.countAvailableByProductType(com.ozerler.marble.model.enums.StockProductType.SIZED);
        BigDecimal factorySizedItemAreaM2 = stockItemRepository.sumQuantityByProductType(com.ozerler.marble.model.enums.StockProductType.SIZED);

        BigDecimal totalEstTon = blockRepository.sumTotalEstimatedTonnage();
        BigDecimal totalActTon = blockRepository.sumTotalActualTonnage();

        long openOrders = workOrderRepository.countByStatus(com.ozerler.marble.model.enums.OperationWorkOrderStatus.NEW)
                + workOrderRepository.countByStatus(com.ozerler.marble.model.enums.OperationWorkOrderStatus.APPROVED);
        long inProdOrders = workOrderRepository.countByStatus(com.ozerler.marble.model.enums.OperationWorkOrderStatus.IN_PRODUCTION);
        long completedOrders = workOrderRepository.countByStatus(com.ozerler.marble.model.enums.OperationWorkOrderStatus.COMPLETED);

        java.time.LocalDate monthStart = java.time.LocalDate.now().withDayOfMonth(1);
        java.time.LocalDate monthEnd = java.time.LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);

        BigDecimal monthlyPurchases = invoiceRepository.sumAmountByTypeThisMonth(com.ozerler.marble.model.enums.InvoiceType.PURCHASE, monthStart, monthEnd);
        BigDecimal monthlySales = invoiceRepository.sumAmountByTypeThisMonth(com.ozerler.marble.model.enums.InvoiceType.SALES, monthStart, monthEnd);
        BigDecimal monthlyCollections = collectionRepository.sumAmountByMethodAndDateRange(null, monthStart, monthEnd);

        java.time.LocalDate today = java.time.LocalDate.now();
        long approachingChecksCount = checkRepository.countApproachingChecks(today, today.plusDays(15));
        BigDecimal approachingChecksTotal = checkRepository.sumApproachingChecksAmount(today, today.plusDays(15));

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
                .quarryBlockCount(quarryBlockCount)
                .factoryBlockUncutCount(factoryBlockUncutCount)
                .factorySizedItemCount(factorySizedItemCount)
                .factorySizedItemAreaM2(factorySizedItemAreaM2)
                .totalEstimatedTonnage(totalEstTon)
                .totalActualTonnage(totalActTon)
                .openWorkOrdersCount(openOrders)
                .inProductionWorkOrdersCount(inProdOrders)
                .completedWorkOrdersCount(completedOrders)
                .monthlyPurchasesTotal(monthlyPurchases)
                .monthlySalesTotal(monthlySales)
                .monthlyCollectionsTotal(monthlyCollections)
                .approachingChecksCount(approachingChecksCount)
                .approachingChecksTotal(approachingChecksTotal)
                .build();
    }
}
