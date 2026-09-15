package com.ozerler.marble.domain;

import com.ozerler.marble.model.enums.QuantityUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationYieldTest {

    @Test
    @DisplayName("same-unit operations reject output plus waste above input")
    void sameUnitBalance() {
        OperationYield.validateSameUnitBalance(new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("2"));
        assertThatThrownBy(() -> OperationYield.validateSameUnitBalance(
                new BigDecimal("10"), new BigDecimal("9"), new BigDecimal("2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("polishing yield is output over input percent")
    void yieldPercent() {
        assertThat(OperationYield.yieldPercent(new BigDecimal("100"), new BigDecimal("92")))
                .isEqualByComparingTo("92.0");
        assertThat(OperationYield.squareMetersPerTon(new BigDecimal("48.5"), new BigDecimal("24.3")))
                .isEqualByComparingTo("1.9959");
        assertThat(OperationYield.samePhysicalUnit(QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER))
                .isTrue();
        assertThat(OperationYield.samePhysicalUnit(QuantityUnit.TON, QuantityUnit.SQUARE_METER, QuantityUnit.KG))
                .isFalse();
    }
}
