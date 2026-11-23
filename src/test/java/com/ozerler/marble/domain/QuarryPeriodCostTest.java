package com.ozerler.marble.domain;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuarryPeriodCostTest {

    @Test
    @DisplayName("cost per ton is period expense divided by produced tons")
    void costPerTon() {
        var result = QuarryPeriodCost.calculate("2026-08", new BigDecimal("150000"), new BigDecimal("75"));
        Assertions.assertThat(result.unallocatedCarryForward()).isFalse();
        Assertions.assertThat(result.costPerTon()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("zero production does not divide and marks unallocated carry-forward")
    void zeroProduction() {
        var result = QuarryPeriodCost.calculate("2026-09", new BigDecimal("12000"), BigDecimal.ZERO);
        Assertions.assertThat(result.unallocatedCarryForward()).isTrue();
        Assertions.assertThat(result.costPerTon()).isNull();
        Assertions.assertThat(result.totalExpense()).isEqualByComparingTo("12000");
    }
}
