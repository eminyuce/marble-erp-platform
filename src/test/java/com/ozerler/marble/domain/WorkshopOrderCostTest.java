package com.ozerler.marble.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkshopOrderCostTest {

    @Test
    @DisplayName("workshop order cost sums material, machine, labor, consumable, scrap and freight")
    void sumsComponents() {
        BigDecimal total = WorkshopOrderCost.total(
                new BigDecimal("1000"),
                new BigDecimal("200"),
                new BigDecimal("150"),
                new BigDecimal("50"),
                new BigDecimal("80"),
                new BigDecimal("20"));
        assertThat(total).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("null components count as zero and negatives are rejected")
    void nullAndNegative() {
        assertThat(WorkshopOrderCost.total(null, null, null, null, null, null))
                .isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> WorkshopOrderCost.total(new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
