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
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CostAccountingService {

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
                identifier != null ? identifier : "MAMUL-STANDARD",
                stoneType != null ? stoneType : "Doğal Mermer",
                rawBlockCostM2 != null ? rawBlockCostM2 : new BigDecimal("820.00"),
                factoryProductionM2 != null ? factoryProductionM2 : new BigDecimal("210.00"),
                workshopFabricationM2 != null ? workshopFabricationM2 : new BigDecimal("165.00"),
                scrapBurdenM2 != null ? scrapBurdenM2 : new BigDecimal("95.00"),
                logisticsM2 != null ? logisticsM2 : new BigDecimal("45.00"),
                generalOverheadM2 != null ? generalOverheadM2 : new BigDecimal("30.00"),
                targetMarginPct != null ? targetMarginPct : new BigDecimal("30.00")
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
