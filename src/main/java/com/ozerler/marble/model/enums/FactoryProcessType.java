package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum FactoryProcessType {
    ST_CUTTING("enum.factory_process_type.st_cutting", QuantityUnit.TON, QuantityUnit.SQUARE_METER),
    GANGSAW_CUTTING("enum.factory_process_type.gangsaw_cutting", QuantityUnit.TON, QuantityUnit.SQUARE_METER),
    SLAB_POLISHING("enum.factory_process_type.slab_polishing", QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER),
    STRIP_POLISHING("enum.factory_process_type.strip_polishing", QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER),
    BRIDGE_SAW_SIZING("enum.factory_process_type.bridge_saw_sizing", QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER),
    PALLETIZING("enum.factory_process_type.palletizing", QuantityUnit.SQUARE_METER, QuantityUnit.SQUARE_METER);

    private final String messageKey;
    private final QuantityUnit defaultInputUnit;
    private final QuantityUnit defaultOutputUnit;

    FactoryProcessType(String messageKey, QuantityUnit defaultInputUnit, QuantityUnit defaultOutputUnit) {
        this.messageKey = messageKey;
        this.defaultInputUnit = defaultInputUnit;
        this.defaultOutputUnit = defaultOutputUnit;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public QuantityUnit getDefaultInputUnit() {
        return defaultInputUnit;
    }

    public QuantityUnit getDefaultOutputUnit() {
        return defaultOutputUnit;
    }

    public boolean usesSamePhysicalUnit() {
        return defaultInputUnit == defaultOutputUnit;
    }

    public ProcessType toLegacyProcessType() {
        return switch (this) {
            case ST_CUTTING -> ProcessType.ST;
            case GANGSAW_CUTTING -> ProcessType.GANGSAW;
            case SLAB_POLISHING -> ProcessType.POLISHING;
            case STRIP_POLISHING -> ProcessType.STRIP_POLISHING;
            case BRIDGE_SAW_SIZING -> ProcessType.BRIDGE_CUTTING;
            case PALLETIZING -> ProcessType.PALLETIZING;
        };
    }
}
