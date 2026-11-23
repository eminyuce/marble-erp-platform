package com.ozerler.marble.domain;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.enums.QuantityUnit;

class OperationYieldTest {

    @Test
    @DisplayName("same-unit operations reject output plus waste above input")
    void sameUnitBalance() {
        OperationYield.validateSameUnitBalance(new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("2"));
        Assertions.assertThatThrownBy(() -> OperationYield.validateSameUnitBalance(
                new BigDecimal("10"), new BigDecimal("9"), new BigDecimal("2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("polishing yield is output over input percent")
    void yieldPercent() {
        Assertions.assertThat(OperationYield.yieldPercent(new BigDecimal("100"), new BigDecimal("92")))
                .isEqualByComparingTo("92.0");
        Assertions.assertThat(OperationYield.squareMetersPerTon(new BigDecimal("48.5"), new BigDecimal("24.3")))
                .isEqualByComparingTo("1.9959");
        Assertions.assertThat(OperationYield.samePhysicalUnit(QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER))
                .isTrue();
        Assertions.assertThat(OperationYield.samePhysicalUnit(QuantityUnit.TON, QuantityUnit.SQUARE_METER, QuantityUnit.KG))
                .isFalse();
    }
}
