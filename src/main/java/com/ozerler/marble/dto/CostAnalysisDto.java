package com.ozerler.marble.dto;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class CostAnalysisDto {
    BusinessUnit businessUnit;
    String businessUnitLabel;
    String expensePeriod;
    String previousPeriod;
    BigDecimal totalExpense;
    BigDecimal previousExpense;
    BigDecimal productionQuantity;
    String productionUnit;
    BigDecimal unitCost;
    BigDecimal previousUnitCost;
    boolean unallocatedCarryForward;
    BigDecimal incomingBlockCost;
    Map<ExpenseCategory, BigDecimal> expenseByCategory;
    List<YieldRow> yields;
    List<Line> lines;

    public BigDecimal calculateUnitContribution(BigDecimal amount) {
        if (amount == null || productionQuantity == null || productionQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return amount.divide(productionQuantity, 2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCategorySharePct(BigDecimal amount) {
        if (amount == null || totalExpense == null || totalExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(totalExpense, 1, java.math.RoundingMode.HALF_UP);
    }

    @Value
    @Builder
    public static class YieldRow {
        String processLabel;
        BigDecimal inputQuantity;
        String inputUnit;
        BigDecimal outputQuantity;
        String outputUnit;
        BigDecimal wasteQuantity;
        String wasteUnit;
        BigDecimal yieldValue;
        String yieldLabel;
    }

    @Value
    @Builder
    public static class Line {
        Long id;
        String categoryLabel;
        String description;
        BigDecimal amount;
        String documentNo;
    }
}
