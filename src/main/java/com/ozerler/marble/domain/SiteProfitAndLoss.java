package com.ozerler.marble.domain;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.util.MessageUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Construction-site profit and loss from movement totals, not an incremental column.
 */
public final class SiteProfitAndLoss {

    private SiteProfitAndLoss() {
    }

    public static Result calculate(
            BigDecimal installedMaterialCost,
            BigDecimal laborCost,
            BigDecimal taxCost,
            BigDecimal consumableCost,
            BigDecimal transportationCost,
            BigDecimal otherCost,
            BigDecimal realizedRevenue
    ) {
        BigDecimal totalCost = sum(
                installedMaterialCost, laborCost, taxCost, consumableCost, transportationCost, otherCost);
        BigDecimal revenue = zeroIfNull(realizedRevenue);
        return new Result(totalCost, revenue, revenue.subtract(totalCost).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP));
    }

    private static BigDecimal sum(BigDecimal... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            total = total.add(zeroIfNull(amount));
        }
        return total.setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.cost.negative_amount"));
        }
        return value != null ? value : BigDecimal.ZERO;
    }

    public record Result(BigDecimal totalCost, BigDecimal realizedRevenue, BigDecimal netProfitOrLoss) {
        public Result {
            Objects.requireNonNull(totalCost);
            Objects.requireNonNull(realizedRevenue);
            Objects.requireNonNull(netProfitOrLoss);
        }
    }
}
