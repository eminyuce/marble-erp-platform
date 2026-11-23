package com.ozerler.marble.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

import com.ozerler.marble.common.Constants;

/**
 * Palet/plaka kümülatif maliyeti: önceki adımın toplamı + bu adımın gerçek gideri,
 * kalan alana (fire düşülerek) yayılır.
 */
public final class PalletCostAccumulator {

    private PalletCostAccumulator() {
    }

    public record AreaCost(BigDecimal areaM2, BigDecimal costPerM2) {
    }

    public record StepResult(
            BigDecimal previousTotalCost,
            BigDecimal addedCost,
            BigDecimal remainingAreaM2,
            BigDecimal newTotalCost,
            BigDecimal newCostPerM2
    ) {
    }

    public static StepResult applyOperation(BigDecimal currentCostPerM2,
                                            BigDecimal currentAreaM2,
                                            BigDecimal remainingAreaM2,
                                            BigDecimal operationCost) {
        BigDecimal previousCostPerM2 = zero(currentCostPerM2);
        BigDecimal inputArea = positiveOrZero(currentAreaM2);
        BigDecimal remaining = remainingAreaOrInput(remainingAreaM2, inputArea);
        BigDecimal added = zero(operationCost);
        BigDecimal previousTotal = money(previousCostPerM2.multiply(inputArea));
        BigDecimal newTotal = money(previousTotal.add(added));
        BigDecimal newUnit;
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            newUnit = newTotal.divide(remaining, Constants.COST_SCALE, RoundingMode.HALF_UP);
        } else {
            newUnit = previousCostPerM2;
        }
        return new StepResult(previousTotal, added, remaining, newTotal, newUnit);
    }

    public static BigDecimal weightedAverageCostPerM2(Collection<AreaCost> lines) {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (AreaCost line : lines) {
            if (line == null) {
                continue;
            }
            BigDecimal area = positiveOrZero(line.areaM2());
            totalArea = totalArea.add(area);
            totalCost = totalCost.add(zero(line.costPerM2()).multiply(area));
        }
        if (totalArea.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(totalArea, Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal remainingAreaOrInput(BigDecimal remainingAreaM2, BigDecimal inputArea) {
        BigDecimal positiveOrZeroValue;
        if (remainingAreaM2 != null) {
            positiveOrZeroValue = remainingAreaM2;
        } else {
            positiveOrZeroValue = inputArea;
        }
        return positiveOrZero(positiveOrZeroValue);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal value) {
        if (value != null) {
            return value;
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal positiveOrZero(BigDecimal value) {
        BigDecimal safe = zero(value);
        if (safe.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return safe;
    }

    public static AreaCost line(BigDecimal areaM2, BigDecimal costPerM2) {
        Objects.requireNonNull(areaM2, "areaM2");
        return new AreaCost(areaM2, costPerM2);
    }
}
