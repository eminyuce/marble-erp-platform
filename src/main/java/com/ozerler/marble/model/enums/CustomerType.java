package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum CustomerType {
    MARBLE_APPLICATION("enum.customer_type.marble_application"),
    CONSTRUCTION("enum.customer_type.construction"),
    DEALER("enum.customer_type.dealer");

    private final String messageKey;

    CustomerType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
