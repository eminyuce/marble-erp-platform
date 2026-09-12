package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ConsumptionType {
    STONE("enum.consumption_type.stone"),
    ADHESIVE("enum.consumption_type.adhesive"),
    GROUT("enum.consumption_type.grout"),
    CHEMICAL("enum.consumption_type.chemical"),
    LABOR("enum.consumption_type.labor");

    private final String messageKey;

    ConsumptionType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
