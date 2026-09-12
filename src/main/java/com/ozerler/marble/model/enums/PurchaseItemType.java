package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum PurchaseItemType {
    CONSUMABLE("enum.purchase_item_type.consumable"),
    ADHESIVE("enum.purchase_item_type.adhesive"),
    GROUT("enum.purchase_item_type.grout"),
    CHEMICAL("enum.purchase_item_type.chemical"),
    STONE("enum.purchase_item_type.stone"),
    EQUIPMENT("enum.purchase_item_type.equipment"),
    SAND("enum.purchase_item_type.sand"),
    CEMENT("enum.purchase_item_type.cement");

    private final String messageKey;

    PurchaseItemType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
