package com.ozerler.marble.domain;

import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.util.MessageUtils;

import java.util.List;
import java.util.Objects;

/**
 * ST kesim yalnızca Dar Bant Cila'ya, Katrak kesim yalnızca Plaka Cila'ya gidebilir.
 * Köprü testere ebatlama her iki kesimden sonra da serbesttir.
 */
public final class FactoryProcessRouting {

    public static final List<FactoryProcessType> CUTTING_TYPES = List.of(
            FactoryProcessType.ST_CUTTING,
            FactoryProcessType.GANGSAW_CUTTING);

    public static final List<FactoryProcessType> SURFACE_TYPES = List.of(
            FactoryProcessType.SLAB_POLISHING,
            FactoryProcessType.STRIP_POLISHING,
            FactoryProcessType.BRIDGE_SAW_SIZING);

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
        return CUTTING_TYPES.contains(processType);
    }

    public static boolean isPolishing(FactoryProcessType processType) {
        return processType == FactoryProcessType.SLAB_POLISHING
                || processType == FactoryProcessType.STRIP_POLISHING;
    }

    public static boolean isSurface(FactoryProcessType processType) {
        return SURFACE_TYPES.contains(processType);
    }

    public static List<FactoryProcessType> allowedSurfaceTypes(FactoryProcessType previousCutting) {
        FactoryProcessType requiredPolishing = requiredPolishingFor(previousCutting);
        if (requiredPolishing == null) {
            return SURFACE_TYPES;
        }
        return List.of(requiredPolishing, FactoryProcessType.BRIDGE_SAW_SIZING);
    }

    public static void requireCutting(FactoryProcessType processType) {
        if (!isCutting(processType)) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.factory.process.not_cutting"));
        }
    }

    public static void requireSurface(FactoryProcessType processType) {
        if (!isSurface(processType)) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.factory.process.not_surface"));
        }
    }

    public static void requireValidSurfaceRouting(FactoryProcessType previousCutting, FactoryProcessType requested) {
        Objects.requireNonNull(requested, MessageUtils.getMessage("error.factory.process.required"));
        if (!isPolishing(requested)) {
            return;
        }
        FactoryProcessType requiredPolishing = requiredPolishingFor(previousCutting);
        if (requiredPolishing == null || requiredPolishing != requested) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.factory.process.invalid_routing"));
        }
    }
}
