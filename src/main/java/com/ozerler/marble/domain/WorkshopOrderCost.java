package com.ozerler.marble.domain;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.util.MessageUtils;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Workshop work-order cost from recorded material, machine, labor, consumable, scrap and freight.
 */
public final class WorkshopOrderCost {

    private WorkshopOrderCost() {
    }

    public static BigDecimal total(
            BigDecimal sourceMaterialCost,
            BigDecimal machineUsageCost,
            BigDecimal manualLaborCost,
            BigDecimal consumableCost,
            BigDecimal scrapShare,
            BigDecimal freightCost
    ) {
        return nonNegative("sourceMaterialCost", sourceMaterialCost)
                .add(nonNegative("machineUsageCost", machineUsageCost))
                .add(nonNegative("manualLaborCost", manualLaborCost))
                .add(nonNegative("consumableCost", consumableCost))
                .add(nonNegative("scrapShare", scrapShare))
                .add(nonNegative("freightCost", freightCost))
                .setScale(Constants.COST_SCALE, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal nonNegative(String field, BigDecimal value) {
        BigDecimal amount = value != null ? value : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.cost.negative_amount") + ": " + field);
        }
        Objects.requireNonNull(field);
        return amount;
    }
}
