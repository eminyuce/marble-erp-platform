package com.ozerler.marble.service;

import com.ozerler.marble.domain.BlockCostFormula;
import com.ozerler.marble.domain.BlockMeasurement;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.dto.QuarryCostBreakdownDto;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BlockCostCalculationService {

    private final BlockRepository blockRepository;
    private final CostTransactionRepository costTransactionRepository;

    @Transactional(readOnly = true)
    public QuarryCostBreakdownDto quarryBreakdown(Long quarryId, String period) {
        String normalized = ExpensePeriods.normalize(period != null ? period : YearMonth.now().toString());
        BigDecimal totalExpenses = zero(costTransactionRepository.sumByQuarryAndPeriod(quarryId, normalized));
        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            totalExpenses = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.QUARRY, normalized));
        }

        List<Block> blocks = blockRepository.findByQuarryId(quarryId);
        BigDecimal totalTonnage = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        for (Block block : blocks) {
            BigDecimal tonnage = blockTonnage(block);
            totalTonnage = totalTonnage.add(tonnage);
            BigDecimal market = BlockCostFormula.blockMarketValue(
                    tonnage, block.getSalePrice(), block.getUnitMarketValuePerTon());
            if (market != null) {
                totalMarketValue = totalMarketValue.add(market);
            }
        }

        BigDecimal expensePerTon = BlockCostFormula.expensePerTon(totalExpenses, totalTonnage);
        BigDecimal avgMarket = BlockCostFormula.averageMarketValuePerTon(totalMarketValue, totalTonnage);
        BigDecimal marginPerTon = BlockCostFormula.marginPerTon(avgMarket, totalExpenses, totalTonnage);

        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        for (Object[] row : costTransactionRepository.sumByTypeForQuarryAndPeriod(quarryId, normalized)) {
            ExpenseType type = (ExpenseType) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            byType.put(type.getLabel(), amount);
        }
        if (byType.isEmpty()) {
            for (Object[] row : costTransactionRepository.sumByTypeForUnitAndPeriod(BusinessUnit.QUARRY, normalized)) {
                ExpenseType type = (ExpenseType) row[0];
                BigDecimal amount = (BigDecimal) row[1];
                byType.put(type.getLabel(), amount);
            }
        }

        String quarryName = blocks.stream()
                .map(b -> b.getQuarry() != null ? b.getQuarry().getName() : null)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse("Ocak");

        return QuarryCostBreakdownDto.builder()
                .quarryId(quarryId)
                .quarryName(quarryName)
                .expensePeriod(normalized)
                .totalExpenses(totalExpenses)
                .totalTonnage(totalTonnage)
                .totalMarketValue(totalMarketValue)
                .expensePerTon(expensePerTon)
                .avgMarketValuePerTon(avgMarket)
                .marginPerTon(marginPerTon)
                .expenseByType(byType)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> expensePerTonByQuarry(String period) {
        Map<Long, BigDecimal> cache = new ConcurrentHashMap<>();
        for (Long quarryId : blockRepository.findDistinctQuarryIds()) {
            QuarryCostBreakdownDto breakdown = quarryBreakdown(quarryId, period);
            if (breakdown.getExpensePerTon() != null) {
                cache.put(quarryId, breakdown.getExpensePerTon());
            }
        }
        return cache;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatedExtractionCost(Block block, BigDecimal expensePerTon) {
        if (block == null) {
            return BigDecimal.ZERO;
        }
        return BlockCostFormula.blockExtractionCost(blockTonnage(block), expensePerTon);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatedTotalCost(Block block, BigDecimal expensePerTon) {
        BigDecimal extraction = calculatedExtractionCost(block, expensePerTon);
        BigDecimal transport = block.getTransportCost() != null ? block.getTransportCost() : BigDecimal.ZERO;
        return extraction.add(transport);
    }

    public BigDecimal blockTonnage(Block block) {
        return BlockMeasurement.productionTonnage(block.getActualWeightKg(), block.getTheoreticalWeightKg());
    }

    public BigDecimal blockMarketValue(Block block) {
        return BlockCostFormula.blockMarketValue(
                blockTonnage(block), block.getSalePrice(), block.getUnitMarketValuePerTon());
    }

    public BigDecimal blockFootprintM2(Block block) {
        return BlockCostFormula.footprintAreaM2(block.getWidthCm(), block.getLengthCm());
    }

    private static BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
