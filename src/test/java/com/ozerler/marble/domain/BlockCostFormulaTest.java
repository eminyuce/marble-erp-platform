package com.ozerler.marble.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BlockCostFormulaTest {

    @Test
    @DisplayName("expense per ton divides total expenses by tonnage")
    void expensePerTon() {
        assertThat(BlockCostFormula.expensePerTon(new BigDecimal("100000"), new BigDecimal("50")))
                .isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("block extraction cost allocates by tonnage")
    void blockExtractionCost() {
        assertThat(BlockCostFormula.blockExtractionCost(new BigDecimal("20"), new BigDecimal("2000")))
                .isEqualByComparingTo("40000.00");
    }

    @Test
    @DisplayName("sold block market value uses sale price")
    void marketValueFromSalePrice() {
        assertThat(BlockCostFormula.blockMarketValue(new BigDecimal("20"), new BigDecimal("800000"), null))
                .isEqualByComparingTo("800000");
    }

    @Test
    @DisplayName("unsold block market value uses unit per ton")
    void marketValueFromUnitPerTon() {
        assertThat(BlockCostFormula.blockMarketValue(new BigDecimal("20"), null, new BigDecimal("35000")))
                .isEqualByComparingTo("700000.00");
    }

    @Test
    @DisplayName("footprint m2 from width and length")
    void footprintArea() {
        assertThat(BlockCostFormula.footprintAreaM2(200, 250))
                .isEqualByComparingTo("5.00");
    }
}
