package com.ozerler.marble.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockMeasurementTest {

    @Test
    @DisplayName("volume and approximate tonnage follow the quarry formula")
    void volumeAndApproximateTonnage() {
        BigDecimal volume = BlockMeasurement.volumeCubicMeters(175, 285, 180);
        assertThat(volume).isEqualByComparingTo(
                BigDecimal.valueOf(175L * 285 * 180)
                        .divide(BlockMeasurement.CUBIC_CENTIMETERS_PER_CUBIC_METER, 3, java.math.RoundingMode.HALF_UP));
        BigDecimal tons = BlockMeasurement.approximateTonnage(volume, new BigDecimal("2.70"));
        assertThat(tons).isEqualByComparingTo(volume.multiply(new BigDecimal("2.70")).setScale(3, java.math.RoundingMode.HALF_UP));
        assertThat(BlockMeasurement.theoreticalWeightKg(tons))
                .isEqualByComparingTo(tons.multiply(new BigDecimal("1000")).setScale(2));
    }

    @Test
    @DisplayName("kantar deviation warning uses a 5 percent absolute threshold")
    void deviationWarning() {
        BigDecimal theoretical = new BigDecimal("27000.00");
        BigDecimal actual = new BigDecimal("25000.00");
        BigDecimal deviation = BlockMeasurement.weightDeviationPercent(actual, theoretical);
        assertThat(deviation.abs()).isGreaterThan(new BigDecimal("5.00"));
        assertThat(BlockMeasurement.exceedsDeviationWarning(deviation)).isTrue();
        assertThat(BlockMeasurement.exceedsDeviationWarning(new BigDecimal("5.00"))).isFalse();
    }

    @Test
    @DisplayName("production tonnage prefers actual weighbridge weight")
    void productionTonnagePrefersActual() {
        assertThat(BlockMeasurement.productionTonnage(new BigDecimal("27000"), new BigDecimal("26000")))
                .isEqualByComparingTo("27.000");
        assertThat(BlockMeasurement.productionTonnage(BigDecimal.ZERO, new BigDecimal("26000")))
                .isEqualByComparingTo("26.000");
    }

    @Test
    @DisplayName("dimensions and density must be positive")
    void rejectsNonPositiveInput() {
        assertThatThrownBy(() -> BlockMeasurement.volumeCubicMeters(0, 100, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BlockMeasurement.approximateTonnage(BigDecimal.ONE, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
