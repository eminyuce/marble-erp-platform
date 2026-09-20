package com.ozerler.marble.domain;

import com.ozerler.marble.common.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

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
        BigDecimal remaining = remainingAreaM2 != null ? remainingAreaM2 : inputArea;
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        BigDecimal added = zero(operationCost);
        BigDecimal previousTotal = previousCostPerM2.multiply(inputArea).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal newTotal = previousTotal.add(added).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal newUnit = remaining.compareTo(BigDecimal.ZERO) > 0
                ? newTotal.divide(remaining, Constants.COST_SCALE, RoundingMode.HALF_UP)
                : previousCostPerM2;
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

    private static BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal positiveOrZero(BigDecimal value) {
        BigDecimal safe = zero(value);
        return safe.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : safe;
    }

    public static AreaCost line(BigDecimal areaM2, BigDecimal costPerM2) {
        Objects.requireNonNull(areaM2, "areaM2");
        return new AreaCost(areaM2, costPerM2);
    }
}
