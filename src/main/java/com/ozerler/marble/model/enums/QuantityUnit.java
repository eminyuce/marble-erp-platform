package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum QuantityUnit {
    TON("enum.quantity_unit.ton"),
    KG("enum.quantity_unit.kg"),
    SQUARE_METER("enum.quantity_unit.square_meter"),
    PIECE("enum.quantity_unit.piece");

    private final String messageKey;

    QuantityUnit(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
