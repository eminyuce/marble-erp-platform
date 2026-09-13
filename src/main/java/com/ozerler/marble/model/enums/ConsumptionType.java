package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ConsumptionType {
    STONE("enum.consumption_type.stone"),
    ADHESIVE("enum.consumption_type.adhesive"),
    GROUT("enum.consumption_type.grout"),
    ANCHORAGE("enum.consumption_type.anchorage"),
    MECHANICAL_ANCHOR("enum.consumption_type.mechanical_anchor"),
    CHEMICAL("enum.consumption_type.chemical"),
    SEALANT("enum.consumption_type.sealant"),
    LABOR("enum.consumption_type.labor"),
    MISCELLANEOUS("enum.consumption_type.miscellaneous");

    private final String messageKey;

    ConsumptionType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
