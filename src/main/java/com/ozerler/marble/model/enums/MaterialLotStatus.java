package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum MaterialLotStatus {
    AVAILABLE("enum.material_lot_status.available"),
    RESERVED("enum.material_lot_status.reserved"),
    IN_PROCESS("enum.material_lot_status.in_process"),
    PALLETIZED("enum.material_lot_status.palletized"),
    SHIPPED("enum.material_lot_status.shipped"),
    INSTALLED("enum.material_lot_status.installed"),
    SCRAPPED("enum.material_lot_status.scrapped");

    private final String messageKey;

    MaterialLotStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
