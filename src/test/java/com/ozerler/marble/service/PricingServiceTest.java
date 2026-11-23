package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.common.Constants;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
    }

    @Test
    @DisplayName("simulatePrice should calculate base price and margin correctly without discount")
    void simulatePriceStandardInputsCalculatesAccurately() {
        BigDecimal unitCost = new BigDecimal("1000.00");
        BigDecimal targetMarginPct = new BigDecimal("20.00"); // 1000 / (1 - 0.20) = 1250.00
        BigDecimal discountPct = BigDecimal.ZERO;

        Map<String, Object> result = pricingService.simulatePrice(unitCost, targetMarginPct, discountPct);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get("unitCost")).isEqualTo(unitCost);
        Assertions.assertThat(result.get("suggestedBasePrice")).isEqualTo(new BigDecimal("1250.00"));
        Assertions.assertThat(result.get("netSellingPrice")).isEqualTo(new BigDecimal("1250.00"));
        Assertions.assertThat(result.get("grossProfitPerM2")).isEqualTo(new BigDecimal("250.00"));
        Assertions.assertThat(result.get("resultingMarginPct")).isEqualTo(new BigDecimal("20.0"));
        Assertions.assertThat((Boolean) result.get("marginAlert")).isTrue(); // 20.0 < 22.0 minimum floor
    }

    @Test
    @DisplayName("calculateSimulation should return immutable record with margin alert false when margin >= 22%")
    void calculateSimulationMarginAboveThresholdNoAlert() {
        BigDecimal unitCost = new BigDecimal("1000.00");
        BigDecimal targetMarginPct = new BigDecimal("30.00"); // 1000 / (1 - 0.30) = 1428.57
        BigDecimal discountPct = BigDecimal.ZERO;

        PricingService.PriceSimulationResult simulation =
                pricingService.calculateSimulation(unitCost, targetMarginPct, discountPct);

        Assertions.assertThat(simulation.isMarginBelowMinimum()).isFalse();
        Assertions.assertThat(simulation.resultingMarginPct()).isGreaterThanOrEqualTo(new BigDecimal("22.0"));
    }

    @Test
    @DisplayName("simulatePrice with null inputs should fallback safely to domain defaults")
    void simulatePriceNullInputsUsesDomainDefaults() {
        Map<String, Object> result = pricingService.simulatePrice(null, null, null);

        Assertions.assertThat(result.get("unitCost")).isEqualTo(Constants.DEFAULT_STANDARD_COST_PER_M2);
        Assertions.assertThat(result.get("targetMarginPct")).isEqualTo(Constants.DEFAULT_TARGET_MARGIN_PCT);
        Assertions.assertThat(result.get("appliedDiscountPct")).isEqualTo(BigDecimal.ZERO);
    }
}
