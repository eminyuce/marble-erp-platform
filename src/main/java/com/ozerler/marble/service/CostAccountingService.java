package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CostAccountingService {

    public static final String DEFAULT_IDENTIFIER = Constants.DEFAULT_IDENTIFIER;
    public static final String DEFAULT_STONE_TYPE = Constants.DEFAULT_STONE_TYPE;
    public static final BigDecimal DEFAULT_RAW_BLOCK_COST_M2 = Constants.DEFAULT_RAW_BLOCK_COST_M2;
    public static final BigDecimal DEFAULT_FACTORY_PRODUCTION_M2 = Constants.DEFAULT_FACTORY_PRODUCTION_M2;
    public static final BigDecimal DEFAULT_WORKSHOP_FABRICATION_M2 = Constants.DEFAULT_WORKSHOP_FABRICATION_M2;
    public static final BigDecimal DEFAULT_SCRAP_BURDEN_M2 = Constants.DEFAULT_SCRAP_BURDEN_M2;
    public static final BigDecimal DEFAULT_LOGISTICS_M2 = Constants.DEFAULT_LOGISTICS_M2;
    public static final BigDecimal DEFAULT_GENERAL_OVERHEAD_M2 = Constants.DEFAULT_GENERAL_OVERHEAD_M2;
    public static final BigDecimal DEFAULT_TARGET_MARGIN_PCT = Constants.DEFAULT_TARGET_MARGIN_PCT;

    private final CostCenterRepository costCenterRepository;
    private final CostTransactionRepository costTransactionRepository;
    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @Cacheable(Constants.CACHE_COST_CENTERS)
    @Transactional(readOnly = true)
    public List<CostCenter> getAllCostCenters() {
        return costCenterRepository.findAll();
    }

    @Transactional
    public CostTransaction recordTransaction(Long centerId, Block block, Slab slab, Project project,
                                             ExpenseType expenseType, BigDecimal amount,
                                             String allocationKey, String description) {
        Objects.requireNonNull(centerId, getMessage("error.cost_center.id.required"));
        Objects.requireNonNull(expenseType, getMessage("error.cost_center.type.required"));
        Objects.requireNonNull(amount, getMessage("error.cost_center.amount.required"));

        CostCenter center = costCenterRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.cost_center.not_found", centerId)));

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
