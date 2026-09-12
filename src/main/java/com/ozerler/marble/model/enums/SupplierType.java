package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum SupplierType {
    CONSUMABLE("enum.supplier_type.consumable"),
    CHEMICAL("enum.supplier_type.chemical"),
    STONE("enum.supplier_type.stone"),
    EQUIPMENT("enum.supplier_type.equipment");

    private final String messageKey;

    SupplierType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
