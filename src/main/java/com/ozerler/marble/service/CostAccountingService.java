package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.dto.CostBreakdownDto;
import com.ozerler.marble.dto.view.CostCenterRef;
import com.ozerler.marble.mapper.CostCenterMapper;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.util.MessageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CostAccountingService {

    private final CostCenterRepository costCenterRepository;
    private final CostCenterMapper costCenterMapper;
    private final CostTransactionRepository costTransactionRepository;
    private final MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        return MessageUtils.getMessage(code, args);
    }

    @Cacheable(Constants.CACHE_COST_CENTERS)
    @Transactional(readOnly = true)
    public List<CostCenter> getAllCostCenters() {
        return BoundedReads.all(costCenterRepository);
    }

    /**
     * Sum of the monthly budgets of all cost centers.
     *
     * @return total monthly budget, zero when none is set
     */
    @Transactional(readOnly = true)
    public BigDecimal totalMonthlyBudget() {
        return BoundedReads.all(costCenterRepository).stream()
                .map(CostCenter::getMonthlyBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

        LocalDate today = LocalDate.now();
        CostTransaction tx = CostTransaction.builder()
                .costCenter(center)
                .block(block)
                .slab(slab)
                .project(project)
                .constructionSite(project)
                .expenseType(expenseType)
                .expenseCategory(expenseType.toCategory())
                .businessUnit(center.getBusinessUnit())
                .amount(amount)
                .currency(Constants.CURRENCY_TRY)
                .entryDate(today)
                .invoiceDate(today)
                .expensePeriod(ExpensePeriods.expensePeriod(expenseType, today, today))
                .postingPeriod(ExpensePeriods.postingPeriod(today))
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
                Objects.requireNonNullElse(identifier, Constants.DEFAULT_IDENTIFIER),
                Objects.requireNonNullElse(stoneType, Constants.DEFAULT_STONE_TYPE),
                Objects.requireNonNullElse(rawBlockCostM2, Constants.DEFAULT_RAW_BLOCK_COST_M2),
                Objects.requireNonNullElse(factoryProductionM2, Constants.DEFAULT_FACTORY_PRODUCTION_M2),
                Objects.requireNonNullElse(workshopFabricationM2, Constants.DEFAULT_WORKSHOP_FABRICATION_M2),
                Objects.requireNonNullElse(scrapBurdenM2, Constants.DEFAULT_SCRAP_BURDEN_M2),
                Objects.requireNonNullElse(logisticsM2, Constants.DEFAULT_LOGISTICS_M2),
                Objects.requireNonNullElse(generalOverheadM2, Constants.DEFAULT_GENERAL_OVERHEAD_M2),
                Objects.requireNonNullElse(targetMarginPct, Constants.DEFAULT_TARGET_MARGIN_PCT)
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

    /**
     * Same as {@code getAllCostCenters}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<CostCenterRef> getAllCostCenterViews() {
        return getAllCostCenters().stream().map(costCenterMapper::toDto).toList();
    }
}
