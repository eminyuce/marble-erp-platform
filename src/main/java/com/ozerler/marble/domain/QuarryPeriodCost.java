package com.ozerler.marble.domain;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.util.MessageUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Quarry monthly cost: period expense / produced tons, with zero-production handling.
 */
public final class QuarryPeriodCost {

    private QuarryPeriodCost() {
    }

    public static QuarryPeriodCostResult calculate(String expensePeriod, BigDecimal periodExpense, BigDecimal producedTons) {
        Objects.requireNonNull(expensePeriod, MessageUtils.getMessage("error.expense.period.required"));
        BigDecimal expense = periodExpense != null ? periodExpense : BigDecimal.ZERO;
        BigDecimal tons = producedTons != null ? producedTons : BigDecimal.ZERO;
        if (expense.compareTo(BigDecimal.ZERO) < 0 || tons.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.cost.negative_amount"));
        }
        if (tons.compareTo(BigDecimal.ZERO) == 0) {
            return new QuarryPeriodCostResult(expensePeriod, expense, tons, null, true);
        }
        BigDecimal costPerTon = expense.divide(tons, Constants.COST_SCALE, RoundingMode.HALF_UP);
        return new QuarryPeriodCostResult(expensePeriod, expense, tons, costPerTon, false);
    }

    public record QuarryPeriodCostResult(
            String expensePeriod,
            BigDecimal totalExpense,
            BigDecimal producedTons,
            BigDecimal costPerTon,
            boolean unallocatedCarryForward
    ) {
    }
}
