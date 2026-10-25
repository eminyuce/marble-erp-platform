package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum CollectionMethod {
    CASH("enum.collection_method.cash", "Nakit"),
    BANK_TRANSFER("enum.collection_method.bank_transfer", "EFT / Havale"),
    CREDIT_CARD("enum.collection_method.credit_card", "Kredi Kartı"),
    CHECK("enum.collection_method.check", "Çek");

    private final String messageKey;
    private final String defaultLabel;

    CollectionMethod(String messageKey, String defaultLabel) {
        this.messageKey = messageKey;
        this.defaultLabel = defaultLabel;
    }

    public String getLabel() {
        try {
            String msg = MessageUtils.getMessage(messageKey);
            if (msg != null && !msg.equals(messageKey) && !msg.isBlank()) {
                return msg;
            }
        } catch (Exception ignored) {
        }
        return defaultLabel;
    }

    public String getDisplayName() {
        return getLabel();
    }
}
