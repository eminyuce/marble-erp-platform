package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum BusinessUnit {
    QUARRY("enum.business_unit.quarry"),
    FACTORY("enum.business_unit.factory"),
    WORKSHOP("enum.business_unit.workshop"),
    SITE("enum.business_unit.site");

    private final String messageKey;

    BusinessUnit(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
