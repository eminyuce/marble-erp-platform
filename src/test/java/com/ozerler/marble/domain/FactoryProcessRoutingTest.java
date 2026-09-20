package com.ozerler.marble.domain;

import com.ozerler.marble.model.enums.FactoryProcessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactoryProcessRoutingTest {

    @Test
    @DisplayName("ST cutting allows only strip polishing")
    void stCuttingAllowsStripPolishing() {
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.ST_CUTTING, FactoryProcessType.STRIP_POLISHING);
        assertThat(FactoryProcessRouting.requiredPolishingFor(FactoryProcessType.ST_CUTTING))
                .isEqualTo(FactoryProcessType.STRIP_POLISHING);
    }

    @Test
    @DisplayName("ST cutting rejects slab polishing")
    void stCuttingRejectsSlabPolishing() {
        assertThatThrownBy(() -> FactoryProcessRouting.requireValidSurfaceRouting(
                FactoryProcessType.ST_CUTTING, FactoryProcessType.SLAB_POLISHING))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Gangsaw cutting allows only slab polishing")
    void gangsawnAllowsSlabPolishing() {
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.GANGSAW_CUTTING, FactoryProcessType.SLAB_POLISHING);
        assertThatThrownBy(() -> FactoryProcessRouting.requireValidSurfaceRouting(
                FactoryProcessType.GANGSAW_CUTTING, FactoryProcessType.STRIP_POLISHING))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("bridge saw is allowed after either cutting type")
    void bridgeSawIsAlwaysAllowed() {
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.ST_CUTTING, FactoryProcessType.BRIDGE_SAW_SIZING);
        FactoryProcessRouting.requireValidSurfaceRouting(FactoryProcessType.GANGSAW_CUTTING, FactoryProcessType.BRIDGE_SAW_SIZING);
        assertThat(FactoryProcessRouting.allowedSurfaceTypes(FactoryProcessType.ST_CUTTING))
                .contains(FactoryProcessType.STRIP_POLISHING, FactoryProcessType.BRIDGE_SAW_SIZING)
                .doesNotContain(FactoryProcessType.SLAB_POLISHING);
    }
}
