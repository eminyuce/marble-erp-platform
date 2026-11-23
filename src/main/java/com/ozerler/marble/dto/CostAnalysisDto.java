package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CostAnalysisDto {
    private BusinessUnit businessUnit;
    private String businessUnitLabel;
    private String expensePeriod;
    private String previousPeriod;
    private BigDecimal totalExpense;
    private BigDecimal previousExpense;
    private BigDecimal productionQuantity;
    private String productionUnit;
    private BigDecimal unitCost;
    private BigDecimal previousUnitCost;
    private boolean unallocatedCarryForward;
    private BigDecimal incomingBlockCost;
    private Map<ExpenseCategory, BigDecimal> expenseByCategory;
    private List<YieldRow> yields;
    private List<Line> lines;

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
        private String processLabel;
        private BigDecimal inputQuantity;
        private String inputUnit;
        private BigDecimal outputQuantity;
        private String outputUnit;
        private BigDecimal wasteQuantity;
        private String wasteUnit;
        private BigDecimal yieldValue;
        private String yieldLabel;
    }

    @Value
    @Builder
    public static class Line {
        private Long id;
        private String categoryLabel;
        private String description;
        private BigDecimal amount;
        private String documentNo;
    }
}
