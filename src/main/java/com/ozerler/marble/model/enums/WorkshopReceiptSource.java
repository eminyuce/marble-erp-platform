package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum WorkshopReceiptSource {
    INTERNAL_FACTORY("enum.workshop_receipt_source.internal_factory"),
    EXTERNAL_FACTORY("enum.workshop_receipt_source.external_factory"),
    LONG_LENGTH_STOCK("enum.workshop_receipt_source.long_length_stock");

    private final String messageKey;

    WorkshopReceiptSource(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
