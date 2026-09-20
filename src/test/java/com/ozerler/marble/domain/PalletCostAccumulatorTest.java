package com.ozerler.marble.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PalletCostAccumulatorTest {

    @Test
    @DisplayName("operation cost plus waste concentrates cost on remaining area")
    void applyOperationIncreasesUnitCost() {
        PalletCostAccumulator.StepResult first = PalletCostAccumulator.applyOperation(
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), BigDecimal.ZERO);
        PalletCostAccumulator.StepResult afterPolish = PalletCostAccumulator.applyOperation(
                first.newCostPerM2(), first.remainingAreaM2(), new BigDecimal("8.00"), new BigDecimal("200.00"));

        assertThat(afterPolish.newCostPerM2()).isNotEqualByComparingTo(first.newCostPerM2());
        assertThat(afterPolish.newTotalCost()).isEqualByComparingTo("1200.00");
        assertThat(afterPolish.newCostPerM2()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("weighted average uses area of each slab")
    void weightedAverage() {
        BigDecimal avg = PalletCostAccumulator.weightedAverageCostPerM2(List.of(
                PalletCostAccumulator.line(new BigDecimal("2"), new BigDecimal("100")),
                PalletCostAccumulator.line(new BigDecimal("6"), new BigDecimal("200"))
        ));
        assertThat(avg).isEqualByComparingTo("175.00");
    }
}
