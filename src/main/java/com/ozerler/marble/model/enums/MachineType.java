package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum MachineType {
    QUARRY_MACHINE("enum.machine_type.quarry_machine", BusinessUnit.QUARRY),
    GANGSAW("enum.machine_type.gangsaw", BusinessUnit.FACTORY),
    ST("enum.machine_type.st", BusinessUnit.FACTORY),
    SLAB_POLISHING("enum.machine_type.slab_polishing", BusinessUnit.FACTORY),
    STRIP_POLISHING("enum.machine_type.strip_polishing", BusinessUnit.FACTORY),
    FACTORY_BRIDGE_SAW("enum.machine_type.factory_bridge_saw", BusinessUnit.FACTORY),
    WORKSHOP_BRIDGE_SAW("enum.machine_type.workshop_bridge_saw", BusinessUnit.WORKSHOP),
    EDGE_CUTTER("enum.machine_type.edge_cutter", BusinessUnit.WORKSHOP),
    CHAMFER_MACHINE("enum.machine_type.chamfer_machine", BusinessUnit.WORKSHOP);

    private final String messageKey;
    private final BusinessUnit businessUnit;

    MachineType(String messageKey, BusinessUnit businessUnit) {
        this.messageKey = messageKey;
        this.businessUnit = businessUnit;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }
}
