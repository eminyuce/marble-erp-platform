package com.ozerler.marble.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class PricingService {

    /**
     * Smart Pricing recommendation and margin simulation (BRD Section 6.3)
     */
    public Map<String, Object> simulatePrice(BigDecimal unitCost, BigDecimal targetMarginPct, BigDecimal discountPct) {
        BigDecimal cost = unitCost != null ? unitCost : new BigDecimal("1365.00");
        BigDecimal margin = targetMarginPct != null ? targetMarginPct : new BigDecimal("30.00");
        BigDecimal discount = discountPct != null ? discountPct : BigDecimal.ZERO;

        // Base Suggested Price = Cost / (1 - Margin)
        BigDecimal marginDec = margin.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.subtract(marginDec);
        BigDecimal basePrice = divisor.compareTo(BigDecimal.ZERO) > 0 ?
                cost.divide(divisor, 2, RoundingMode.HALF_UP) : cost;

        // Discounted net price
        BigDecimal discountDec = discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal netPrice = basePrice.multiply(BigDecimal.ONE.subtract(discountDec)).setScale(2, RoundingMode.HALF_UP);

        // Resulting Gross Profit & Margin
        BigDecimal grossProfit = netPrice.subtract(cost);
        BigDecimal resultingMarginPct = netPrice.compareTo(BigDecimal.ZERO) > 0 ?
                grossProfit.divide(netPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Minimum floor check (Alert if margin drops below 22%)
        boolean marginAlert = resultingMarginPct.compareTo(new BigDecimal("22.0")) < 0;

        Map<String, Object> result = new HashMap<>();
        result.put("unitCost", cost);
        result.put("targetMarginPct", margin);
        result.put("suggestedBasePrice", basePrice);
        result.put("appliedDiscountPct", discount);
        result.put("netSellingPrice", netPrice);
        result.put("grossProfitPerM2", grossProfit);
        result.put("resultingMarginPct", resultingMarginPct);
        result.put("marginAlert", marginAlert);

        return result;
    }
}
