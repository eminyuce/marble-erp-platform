package com.ozerler.marble.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlockMeasurementTest {

    @Test
    @DisplayName("volume and approximate tonnage follow the quarry formula")
    void volumeAndApproximateTonnage() {
        BigDecimal volume = BlockMeasurement.volumeCubicMeters(175, 285, 180);
        Assertions.assertThat(volume).isEqualByComparingTo(
                BigDecimal.valueOf(175L * 285 * 180)
                        .divide(BlockMeasurement.CUBIC_CENTIMETERS_PER_CUBIC_METER, 3, RoundingMode.HALF_UP));
        BigDecimal tons = BlockMeasurement.approximateTonnage(volume, new BigDecimal("2.70"));
        Assertions.assertThat(tons).isEqualByComparingTo(volume.multiply(new BigDecimal("2.70")).setScale(3, RoundingMode.HALF_UP));
        Assertions.assertThat(BlockMeasurement.theoreticalWeightKg(tons))
                .isEqualByComparingTo(tons.multiply(new BigDecimal("1000")).setScale(2));
    }

    @Test
    @DisplayName("kantar deviation warning uses a 5 percent absolute threshold")
    void deviationWarning() {
        BigDecimal theoretical = new BigDecimal("27000.00");
        BigDecimal actual = new BigDecimal("25000.00");
        BigDecimal deviation = BlockMeasurement.weightDeviationPercent(actual, theoretical);
        Assertions.assertThat(deviation.abs()).isGreaterThan(new BigDecimal("5.00"));
        Assertions.assertThat(BlockMeasurement.exceedsDeviationWarning(deviation)).isTrue();
        Assertions.assertThat(BlockMeasurement.exceedsDeviationWarning(new BigDecimal("5.00"))).isFalse();
    }

    @Test
    @DisplayName("production tonnage prefers actual weighbridge weight")
    void productionTonnagePrefersActual() {
        Assertions.assertThat(BlockMeasurement.productionTonnage(new BigDecimal("27000"), new BigDecimal("26000")))
                .isEqualByComparingTo("27.000");
        Assertions.assertThat(BlockMeasurement.productionTonnage(BigDecimal.ZERO, new BigDecimal("26000")))
                .isEqualByComparingTo("26.000");
    }

    @Test
    @DisplayName("dimensions and density must be positive")
    void rejectsNonPositiveInput() {
        Assertions.assertThatThrownBy(() -> BlockMeasurement.volumeCubicMeters(0, 100, 100))
                .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> BlockMeasurement.approximateTonnage(BigDecimal.ONE, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
