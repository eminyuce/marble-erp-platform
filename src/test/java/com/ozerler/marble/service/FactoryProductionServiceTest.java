package com.ozerler.marble.service;

import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.QuantityUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactoryProductionServiceTest {

    @Test
    @DisplayName("ST and Katrak cutting keep ton input and square-meter output without unit conversion")
    void cuttingUsesDifferentPhysicalUnits() {
        assertThat(FactoryProcessType.ST_CUTTING.usesSamePhysicalUnit()).isFalse();
        assertThat(FactoryProcessType.GANGSAW_CUTTING.usesSamePhysicalUnit()).isFalse();
        assertThat(FactoryProcessType.ST_CUTTING.getDefaultInputUnit()).isEqualTo(QuantityUnit.TON);
        assertThat(FactoryProcessType.GANGSAW_CUTTING.getDefaultOutputUnit()).isEqualTo(QuantityUnit.SQUARE_METER);
        assertThat(FactoryProcessType.SLAB_POLISHING.usesSamePhysicalUnit()).isTrue();
        assertThat(FactoryProcessType.ST_CUTTING.toLegacyProcessType().getLabel()).isEqualTo("ST");
        assertThat(FactoryProcessType.GANGSAW_CUTTING.toLegacyProcessType().getLabel()).isEqualTo("Katrak");
    }

    @Test
    @DisplayName("recordCutting rejects non-cutting process types before touching a block")
    void recordCuttingRejectsSurfaceProcess() {
        FactoryProductionService service = new FactoryProductionService(
                null, null, null, null, null, null, null, null, null, null, null, null);
        FactoryProductionService.CuttingRequest request = new FactoryProductionService.CuttingRequest(
                1L, "PO-1", null, "Katrak-01", FactoryProcessType.SLAB_POLISHING,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "Op", null,
                0, 1, 0, 0, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, QuantityUnit.KG, null, null, null);

        assertThatThrownBy(() -> service.recordCutting(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
