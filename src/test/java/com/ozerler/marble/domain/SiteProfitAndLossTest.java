package com.ozerler.marble.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SiteProfitAndLossTest {

    @Test
    @DisplayName("net profit is realized revenue minus site movement totals")
    void netProfit() {
        var result = SiteProfitAndLoss.calculate(
                new BigDecimal("400000"),
                new BigDecimal("80000"),
                new BigDecimal("20000"),
                new BigDecimal("15000"),
                new BigDecimal("10000"),
                new BigDecimal("5000"),
                new BigDecimal("600000"));
        assertThat(result.totalCost()).isEqualByComparingTo("530000.00");
        assertThat(result.netProfitOrLoss()).isEqualByComparingTo("70000.00");
    }

    @Test
    @DisplayName("missing revenue yields a loss equal to total cost")
    void missingRevenueIsLoss() {
        var result = SiteProfitAndLoss.calculate(
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        assertThat(result.netProfitOrLoss()).isEqualByComparingTo("-100.00");
    }
}
