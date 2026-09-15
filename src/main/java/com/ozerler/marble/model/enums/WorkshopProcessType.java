package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum WorkshopProcessType {
    BRIDGE_SAW_SIZING("enum.workshop_process_type.bridge_saw_sizing"),
    EDGE_CUTTING("enum.workshop_process_type.edge_cutting"),
    MACHINE_CHAMFERING("enum.workshop_process_type.machine_chamfering"),
    MANUAL_FINE_CHAMFERING("enum.workshop_process_type.manual_fine_chamfering");

    private final String messageKey;

    WorkshopProcessType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public boolean requiresMachine() {
        return this != MANUAL_FINE_CHAMFERING;
    }

    public boolean requiresLaborHours() {
        return this == MANUAL_FINE_CHAMFERING;
    }
}
