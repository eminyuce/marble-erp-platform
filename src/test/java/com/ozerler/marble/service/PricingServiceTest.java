package com.ozerler.marble.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
    }

    @Test
    @DisplayName("simulatePrice should calculate base price and margin correctly without discount")
    void simulatePrice_StandardInputs_CalculatesAccurately() {
        BigDecimal unitCost = new BigDecimal("1000.00");
        BigDecimal targetMarginPct = new BigDecimal("20.00"); // 1000 / (1 - 0.20) = 1250.00
        BigDecimal discountPct = BigDecimal.ZERO;

        Map<String, Object> result = pricingService.simulatePrice(unitCost, targetMarginPct, discountPct);

        assertThat(result).isNotNull();
        assertThat(result.get("unitCost")).isEqualTo(unitCost);
        assertThat(result.get("suggestedBasePrice")).isEqualTo(new BigDecimal("1250.00"));
        assertThat(result.get("netSellingPrice")).isEqualTo(new BigDecimal("1250.00"));
        assertThat(result.get("grossProfitPerM2")).isEqualTo(new BigDecimal("250.00"));
        assertThat(result.get("resultingMarginPct")).isEqualTo(new BigDecimal("20.0"));
        assertThat((Boolean) result.get("marginAlert")).isTrue(); // 20.0 < 22.0 minimum floor
    }

    @Test
    @DisplayName("calculateSimulation should return immutable record with margin alert false when margin >= 22%")
    void calculateSimulation_MarginAboveThreshold_NoAlert() {
        BigDecimal unitCost = new BigDecimal("1000.00");
        BigDecimal targetMarginPct = new BigDecimal("30.00"); // 1000 / (1 - 0.30) = 1428.57
        BigDecimal discountPct = BigDecimal.ZERO;

        PricingService.PriceSimulationResult simulation = pricingService.calculateSimulation(unitCost, targetMarginPct, discountPct);

        assertThat(simulation.isMarginBelowMinimum()).isFalse();
        assertThat(simulation.resultingMarginPct()).isGreaterThanOrEqualTo(new BigDecimal("22.0"));
    }

    @Test
    @DisplayName("simulatePrice with null inputs should fallback safely to domain defaults")
    void simulatePrice_NullInputs_UsesDomainDefaults() {
        Map<String, Object> result = pricingService.simulatePrice(null, null, null);

        assertThat(result.get("unitCost")).isEqualTo(PricingService.DEFAULT_STANDARD_COST_PER_M2);
        assertThat(result.get("targetMarginPct")).isEqualTo(PricingService.DEFAULT_TARGET_MARGIN_PCT);
        assertThat(result.get("appliedDiscountPct")).isEqualTo(BigDecimal.ZERO);
    }
}
