package com.ozerler.marble.domain;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.enums.QuantityUnit;
import com.ozerler.marble.util.MessageUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Factory and workshop yield rules. Different physical units are never converted.
 */
public final class OperationYield {

    private OperationYield() {
    }

    public static void requireNonNegative(String field, BigDecimal value) {
        Objects.requireNonNull(value, MessageUtils.getMessage("error.operation.quantity.required", field));
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.operation.quantity.negative", field));
        }
    }

    public static void validateSameUnitBalance(BigDecimal input, BigDecimal output, BigDecimal waste) {
        requireNonNegative("input", input);
        requireNonNegative("output", output);
        requireNonNegative("waste", waste);
        if (output.add(waste).compareTo(input) > 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.operation.balance.exceeds_input"));
        }
    }

    public static BigDecimal yieldPercent(BigDecimal input, BigDecimal output) {
        requireNonNegative("input", input);
        requireNonNegative("output", output);
        if (input.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return output.multiply(Constants.PERCENT_DIVISOR)
                .divide(input, Constants.PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal squareMetersPerTon(BigDecimal outputSquareMeters, BigDecimal inputTons) {
        requireNonNegative("outputSquareMeters", outputSquareMeters);
        requireNonNegative("inputTons", inputTons);
        if (inputTons.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return outputSquareMeters.divide(inputTons, Constants.CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean samePhysicalUnit(QuantityUnit inputUnit, QuantityUnit outputUnit, QuantityUnit wasteUnit) {
        return inputUnit != null && inputUnit == outputUnit && inputUnit == wasteUnit;
    }
}
