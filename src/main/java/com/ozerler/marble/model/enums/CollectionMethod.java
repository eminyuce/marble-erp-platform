package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum CollectionMethod {
    CASH("enum.collection_method.cash", "Nakit"),
    BANK_TRANSFER("enum.collection_method.bank_transfer", "EFT / Havale"),
    CHECK("enum.collection_method.check", "Çek");

    private final String messageKey;
    private final String defaultLabel;

    CollectionMethod(String messageKey, String defaultLabel) {
        this.messageKey = messageKey;
        this.defaultLabel = defaultLabel;
    }

    public String getLabel() {
        try {
            return MessageUtils.getMessage(messageKey);
        } catch (Exception e) {
            return defaultLabel;
        }
    }

    public String getDisplayName() {
        return getLabel();
    }
}
