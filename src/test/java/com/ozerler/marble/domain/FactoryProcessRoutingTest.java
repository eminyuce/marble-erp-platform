package com.ozerler.marble.domain;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.exception.RejectedInputException;
import com.ozerler.marble.model.enums.FactoryProcessType;

class FactoryProcessRoutingTest {

    @Test
    @DisplayName("ST cutting allows only strip polishing")
    void stCuttingAllowsStripPolishing() {
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.ST_CUTTING, FactoryProcessType.STRIP_POLISHING);
        Assertions.assertThat(FactoryProcessRouting.requiredPolishingFor(FactoryProcessType.ST_CUTTING))
                .isEqualTo(FactoryProcessType.STRIP_POLISHING);
    }

    @Test
    @DisplayName("ST cutting rejects slab polishing")
    void stCuttingRejectsSlabPolishing() {
        Assertions.assertThatThrownBy(() -> FactoryProcessRouting.requireValidSurfaceRouting(
                FactoryProcessType.ST_CUTTING, FactoryProcessType.SLAB_POLISHING))
                .isInstanceOf(RejectedInputException.class)
                .hasMessage("Bu iş emri ST kesimden geldi. Yalnızca Bant Silim (Dar Bant Cila) seçebilirsiniz.")
                .extracting(ex -> ((RejectedInputException) ex).fieldNames())
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly("processType");
    }

    @Test
    @DisplayName("Gangsaw cutting allows only slab polishing")
    void gangsawnAllowsSlabPolishing() {
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.GANGSAW_CUTTING, FactoryProcessType.SLAB_POLISHING);
        Assertions.assertThatThrownBy(() -> FactoryProcessRouting.requireValidSurfaceRouting(
                FactoryProcessType.GANGSAW_CUTTING, FactoryProcessType.STRIP_POLISHING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bu iş emri Katrak kesimden geldi. Yalnızca Plaka Silim (Plaka Cila) seçebilirsiniz.");
    }

    @Test
    @DisplayName("bridge saw is allowed after either cutting type")
    void bridgeSawIsAlwaysAllowed() {
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.ST_CUTTING, FactoryProcessType.BRIDGE_SAW_SIZING);
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.GANGSAW_CUTTING, FactoryProcessType.BRIDGE_SAW_SIZING);
        Assertions.assertThat(FactoryProcessRouting.allowedSurfaceTypes(FactoryProcessType.ST_CUTTING))
                .contains(FactoryProcessType.STRIP_POLISHING, FactoryProcessType.BRIDGE_SAW_SIZING)
                .doesNotContain(FactoryProcessType.SLAB_POLISHING);
    }
}
