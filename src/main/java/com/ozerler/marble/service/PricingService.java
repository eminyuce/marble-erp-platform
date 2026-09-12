package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class PricingService {

    /**
     * Immutable result holding pricing simulation output
     */
    public record PriceSimulationResult(
            BigDecimal unitCost,
            BigDecimal targetMarginPct,
            BigDecimal suggestedBasePrice,
            BigDecimal appliedDiscountPct,
            BigDecimal netSellingPrice,
            BigDecimal grossProfitPerM2,
            BigDecimal resultingMarginPct,
            boolean isMarginBelowMinimum
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "unitCost", unitCost,
                    "targetMarginPct", targetMarginPct,
                    "suggestedBasePrice", suggestedBasePrice,
                    "appliedDiscountPct", appliedDiscountPct,
                    "netSellingPrice", netSellingPrice,
                    "grossProfitPerM2", grossProfitPerM2,
                    "resultingMarginPct", resultingMarginPct,
                    "marginAlert", isMarginBelowMinimum
            );
        }
    }

    /**
     * Smart Pricing recommendation and margin simulation (BRD Section 6.3)
     */
    public Map<String, Object> simulatePrice(BigDecimal unitCost, BigDecimal targetMarginPct, BigDecimal discountPct) {
        return calculateSimulation(unitCost, targetMarginPct, discountPct).toMap();
    }

    /**
     * Strongly typed domain simulation method adhering to clean code immutability
     */
    public PriceSimulationResult calculateSimulation(BigDecimal unitCost, BigDecimal targetMarginPct, BigDecimal discountPct) {
        BigDecimal cost = unitCost != null ? unitCost : Constants.DEFAULT_STANDARD_COST_PER_M2;
        BigDecimal margin = targetMarginPct != null ? targetMarginPct : Constants.DEFAULT_TARGET_MARGIN_PCT;
        BigDecimal discount = discountPct != null ? discountPct : BigDecimal.ZERO;

        BigDecimal basePrice = calculateBasePrice(cost, margin);
        BigDecimal netPrice = calculateDiscountedPrice(basePrice, discount);
        BigDecimal grossProfit = netPrice.subtract(cost);
        BigDecimal resultingMarginPct = calculateResultingMargin(netPrice, grossProfit);
        boolean isMarginAlert = resultingMarginPct.compareTo(Constants.MINIMUM_ACCEPTABLE_MARGIN_PCT) < 0;

        return new PriceSimulationResult(
                cost,
                margin,
                basePrice,
                discount,
                netPrice,
                grossProfit,
                resultingMarginPct,
                isMarginAlert
        );
    }

    private BigDecimal calculateBasePrice(BigDecimal cost, BigDecimal targetMarginPct) {
        BigDecimal marginFraction = targetMarginPct.divide(Constants.PERCENT_DIVISOR, Constants.CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.subtract(marginFraction);
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return cost;
        }
        return cost.divide(divisor, Constants.CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiscountedPrice(BigDecimal basePrice, BigDecimal discountPct) {
        BigDecimal discountFraction = discountPct.divide(Constants.PERCENT_DIVISOR, Constants.CALCULATION_SCALE, RoundingMode.HALF_UP);
        return basePrice.multiply(BigDecimal.ONE.subtract(discountFraction)).setScale(Constants.CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateResultingMargin(BigDecimal netPrice, BigDecimal grossProfit) {
        if (netPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return grossProfit.divide(netPrice, Constants.CALCULATION_SCALE, RoundingMode.HALF_UP)
                .multiply(Constants.PERCENT_DIVISOR)
                .setScale(Constants.PERCENT_SCALE, RoundingMode.HALF_UP);
    }
}
