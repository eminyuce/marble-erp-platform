package com.ozerler.marble.domain;

import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.util.MessageUtils;

import java.util.List;
import java.util.Objects;

/**
 * ST kesim yalnızca Dar Bant Cila'ya, Katrak kesim yalnızca Plaka Cila'ya gidebilir.
 */
public final class FactoryProcessRouting {

    private FactoryProcessRouting() {
    }

    public static FactoryProcessType requiredPolishingFor(FactoryProcessType cuttingType) {
        if (cuttingType == FactoryProcessType.ST_CUTTING) {
            return FactoryProcessType.STRIP_POLISHING;
        }
        if (cuttingType == FactoryProcessType.GANGSAW_CUTTING) {
            return FactoryProcessType.SLAB_POLISHING;
        }
        return null;
    }

    public static boolean isCutting(FactoryProcessType processType) {
        return processType == FactoryProcessType.ST_CUTTING
                || processType == FactoryProcessType.GANGSAW_CUTTING;
    }

    public static boolean isPolishing(FactoryProcessType processType) {
        return processType == FactoryProcessType.SLAB_POLISHING
                || processType == FactoryProcessType.STRIP_POLISHING;
    }

    public static List<FactoryProcessType> allowedSurfaceTypes(FactoryProcessType previousCutting) {
        FactoryProcessType required = requiredPolishingFor(previousCutting);
        if (required == null) {
            return List.of(FactoryProcessType.SLAB_POLISHING, FactoryProcessType.STRIP_POLISHING,
                    FactoryProcessType.BRIDGE_SAW_SIZING);
        }
        return List.of(required, FactoryProcessType.BRIDGE_SAW_SIZING);
    }

    public static void requireValidSurfaceRouting(FactoryProcessType previousCutting, FactoryProcessType requested) {
        Objects.requireNonNull(requested, MessageUtils.getMessage("error.factory.process.required"));
        if (!isPolishing(requested)) {
            return;
        }
        FactoryProcessType required = requiredPolishingFor(previousCutting);
        if (required == null || required != requested) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.factory.process.invalid_routing"));
        }
    }
}
