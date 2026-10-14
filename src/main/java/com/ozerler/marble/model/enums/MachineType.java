package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum MachineType {
    QUARRY_MACHINE("enum.machine_type.quarry_machine", BusinessUnit.QUARRY, false),
    FORKLIFT("enum.machine_type.forklift", BusinessUnit.FACTORY, false),
    TRUCK("enum.machine_type.truck", BusinessUnit.FACTORY, false),
    GANGSAW("enum.machine_type.gangsaw", BusinessUnit.FACTORY, true),
    ST("enum.machine_type.st", BusinessUnit.FACTORY, true),
    SLAB_POLISHING("enum.machine_type.slab_polishing", BusinessUnit.FACTORY, true),
    STRIP_POLISHING("enum.machine_type.strip_polishing", BusinessUnit.FACTORY, true),
    FACTORY_BRIDGE_SAW("enum.machine_type.factory_bridge_saw", BusinessUnit.FACTORY, true),
    WORKSHOP_BRIDGE_SAW("enum.machine_type.workshop_bridge_saw", BusinessUnit.WORKSHOP, false),
    EDGE_CUTTER("enum.machine_type.edge_cutter", BusinessUnit.WORKSHOP, false),
    CHAMFER_MACHINE("enum.machine_type.chamfer_machine", BusinessUnit.WORKSHOP, false);

    private final String messageKey;
    private final BusinessUnit businessUnit;
    private final boolean electric;

    MachineType(String messageKey, BusinessUnit businessUnit) {
        this(messageKey, businessUnit, false);
    }

    MachineType(String messageKey, BusinessUnit businessUnit, boolean electric) {
        this.messageKey = messageKey;
        this.businessUnit = businessUnit;
        this.electric = electric;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public boolean isElectric() {
        return electric;
    }
}
