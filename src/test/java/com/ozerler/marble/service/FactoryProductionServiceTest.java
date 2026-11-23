package com.ozerler.marble.service;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.QuantityUnit;

class FactoryProductionServiceTest {

    @Test
    @DisplayName("ST and Katrak cutting keep ton input and square-meter output without unit conversion")
    void cuttingUsesDifferentPhysicalUnits() {
        Assertions.assertThat(FactoryProcessType.ST_CUTTING.usesSamePhysicalUnit()).isFalse();
        Assertions.assertThat(FactoryProcessType.GANGSAW_CUTTING.usesSamePhysicalUnit()).isFalse();
        Assertions.assertThat(FactoryProcessType.ST_CUTTING.getDefaultInputUnit()).isEqualTo(QuantityUnit.TON);
        Assertions.assertThat(FactoryProcessType.GANGSAW_CUTTING.getDefaultOutputUnit()).isEqualTo(QuantityUnit.SQUARE_METER);
        Assertions.assertThat(FactoryProcessType.SLAB_POLISHING.usesSamePhysicalUnit()).isTrue();
        Assertions.assertThat(FactoryProcessType.ST_CUTTING.toLegacyProcessType().getLabel()).isEqualTo("ST");
        Assertions.assertThat(FactoryProcessType.GANGSAW_CUTTING.toLegacyProcessType().getLabel()).isEqualTo("Katrak");
    }

    @Test
    @DisplayName("recordCutting rejects non-cutting process types before touching a block")
    void recordCuttingRejectsSurfaceProcess() {
        FactoryProductionService service = new FactoryProductionService(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        FactoryProductionService.CuttingRequest request = new FactoryProductionService.CuttingRequest(
                1L, "PO-1", null, "Katrak-01", FactoryProcessType.SLAB_POLISHING,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "Op", null,
                0, 1, 0, 0, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, QuantityUnit.KG, null, null, null);

        Assertions.assertThatThrownBy(() -> service.recordCutting(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
