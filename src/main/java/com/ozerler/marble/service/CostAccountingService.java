package com.ozerler.marble.service;

import com.ozerler.marble.dto.CostBreakdownDto;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CostAccountingService {

    public static final String DEFAULT_IDENTIFIER = "MAMUL-STANDARD";
    public static final String DEFAULT_STONE_TYPE = "Doğal Mermer";
    public static final BigDecimal DEFAULT_RAW_BLOCK_COST_M2 = new BigDecimal("820.00");
    public static final BigDecimal DEFAULT_FACTORY_PRODUCTION_M2 = new BigDecimal("210.00");
    public static final BigDecimal DEFAULT_WORKSHOP_FABRICATION_M2 = new BigDecimal("165.00");
    public static final BigDecimal DEFAULT_SCRAP_BURDEN_M2 = new BigDecimal("95.00");
    public static final BigDecimal DEFAULT_LOGISTICS_M2 = new BigDecimal("45.00");
    public static final BigDecimal DEFAULT_GENERAL_OVERHEAD_M2 = new BigDecimal("30.00");
    public static final BigDecimal DEFAULT_TARGET_MARGIN_PCT = new BigDecimal("30.00");

    private final CostCenterRepository costCenterRepository;
    private final CostTransactionRepository costTransactionRepository;

    @Transactional(readOnly = true)
    public List<CostCenter> getAllCostCenters() {
        return costCenterRepository.findAll();
    }

    @Transactional
    public CostTransaction recordTransaction(Long centerId, Block block, Slab slab, Project project,
                                             ExpenseType expenseType, BigDecimal amount,
                                             String allocationKey, String description) {
        Objects.requireNonNull(centerId, "Masraf merkezi ID boş olamaz");
        Objects.requireNonNull(expenseType, "Gider türü boş olamaz");
        Objects.requireNonNull(amount, "Gider tutarı boş olamaz");

        CostCenter center = costCenterRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Masraf merkezi bulunamadı: " + centerId));

        CostTransaction tx = CostTransaction.builder()
                .costCenter(center)
                .block(block)
                .slab(slab)
                .project(project)
                .expenseType(expenseType)
                .amount(amount)
                .allocationKey(allocationKey)
                .description(description)
                .build();

        return costTransactionRepository.save(tx);
    }

    /**
     * Calculates the full 6-layer actual unit cost breakdown as per BRD Section 6.2
     */
    public CostBreakdownDto calculateMultiLayerCost(String identifier, String stoneType,
                                                   BigDecimal rawBlockCostM2,
                                                   BigDecimal factoryProductionM2,
                                                   BigDecimal workshopFabricationM2,
                                                   BigDecimal scrapBurdenM2,
                                                   BigDecimal logisticsM2,
                                                   BigDecimal generalOverheadM2,
                                                   BigDecimal targetMarginPct) {

        return CostBreakdownDto.calculateStandard(
                identifier != null ? identifier : DEFAULT_IDENTIFIER,
                stoneType != null ? stoneType : DEFAULT_STONE_TYPE,
                rawBlockCostM2 != null ? rawBlockCostM2 : DEFAULT_RAW_BLOCK_COST_M2,
                factoryProductionM2 != null ? factoryProductionM2 : DEFAULT_FACTORY_PRODUCTION_M2,
                workshopFabricationM2 != null ? workshopFabricationM2 : DEFAULT_WORKSHOP_FABRICATION_M2,
                scrapBurdenM2 != null ? scrapBurdenM2 : DEFAULT_SCRAP_BURDEN_M2,
                logisticsM2 != null ? logisticsM2 : DEFAULT_LOGISTICS_M2,
                generalOverheadM2 != null ? generalOverheadM2 : DEFAULT_GENERAL_OVERHEAD_M2,
                targetMarginPct != null ? targetMarginPct : DEFAULT_TARGET_MARGIN_PCT
        );
    }

    @Transactional(readOnly = true)
    public List<Object[]> getCostDistributionByCenter() {
        return costTransactionRepository.getCostDistributionByCenter();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getCostDistributionByType() {
        return costTransactionRepository.getCostDistributionByType();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses() {
        return costTransactionRepository.getTotalCostAmount();
    }
}
