package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum TonnageBasis {
    ACTUAL_SCALE("enum.tonnage_basis.actual_scale"),
    APPROXIMATE("enum.tonnage_basis.approximate");

    private final String messageKey;

    TonnageBasis(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
