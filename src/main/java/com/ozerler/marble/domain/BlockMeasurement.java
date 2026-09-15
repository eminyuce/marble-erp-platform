package com.ozerler.marble.domain;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.util.MessageUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Central block geometry and tonnage formulas used by lists, details, reports and exports.
 */
public final class BlockMeasurement {

    public static final BigDecimal CUBIC_CENTIMETERS_PER_CUBIC_METER = new BigDecimal("1000000");
    public static final BigDecimal KILOGRAMS_PER_TON = new BigDecimal("1000");
    public static final BigDecimal WEIGHT_DEVIATION_WARNING_PERCENT = new BigDecimal("5.00");

    private BlockMeasurement() {
    }

    public static BigDecimal volumeCubicMeters(int widthCm, int lengthCm, int heightCm) {
        requirePositiveDimension("widthCm", widthCm);
        requirePositiveDimension("lengthCm", lengthCm);
        requirePositiveDimension("heightCm", heightCm);
        return BigDecimal.valueOf((long) widthCm * lengthCm * heightCm)
                .divide(CUBIC_CENTIMETERS_PER_CUBIC_METER, Constants.AREA_SCALE - 1, RoundingMode.HALF_UP);
    }

    public static BigDecimal approximateTonnage(BigDecimal volumeM3, BigDecimal specificGravityTonPerM3) {
        Objects.requireNonNull(volumeM3, MessageUtils.getMessage("error.block.volume.required"));
        Objects.requireNonNull(specificGravityTonPerM3, MessageUtils.getMessage("error.quarry.specific_gravity.required"));
        if (volumeM3.compareTo(BigDecimal.ZERO) <= 0 || specificGravityTonPerM3.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.block.measurement.positive"));
        }
        return volumeM3.multiply(specificGravityTonPerM3).setScale(Constants.TONNAGE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal theoreticalWeightKg(BigDecimal approximateTonnage) {
        Objects.requireNonNull(approximateTonnage, MessageUtils.getMessage("error.block.tonnage.required"));
        return approximateTonnage.multiply(KILOGRAMS_PER_TON).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal kilogramsToTons(BigDecimal kilograms) {
        if (kilograms == null || kilograms.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return kilograms.divide(KILOGRAMS_PER_TON, Constants.TONNAGE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal weightDeviationPercent(BigDecimal actualWeightKg, BigDecimal theoreticalWeightKg) {
        if (!hasActualWeight(actualWeightKg) || theoreticalWeightKg == null
                || theoreticalWeightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return actualWeightKg.subtract(theoreticalWeightKg)
                .divide(theoreticalWeightKg, Constants.CALCULATION_SCALE, RoundingMode.HALF_UP)
                .multiply(Constants.PERCENT_DIVISOR)
                .setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean hasActualWeight(BigDecimal actualWeightKg) {
        return actualWeightKg != null && actualWeightKg.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean exceedsDeviationWarning(BigDecimal deviationPercent) {
        return deviationPercent != null
                && deviationPercent.abs().compareTo(WEIGHT_DEVIATION_WARNING_PERCENT) > 0;
    }

    public static BigDecimal productionTonnage(BigDecimal actualWeightKg, BigDecimal theoreticalWeightKg) {
        if (hasActualWeight(actualWeightKg)) {
            return kilogramsToTons(actualWeightKg);
        }
        return kilogramsToTons(theoreticalWeightKg);
    }

    private static void requirePositiveDimension(String field, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.block.dimension.positive", field));
        }
    }
}
