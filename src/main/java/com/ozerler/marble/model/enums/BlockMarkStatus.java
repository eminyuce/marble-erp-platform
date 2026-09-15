package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum BlockMarkStatus {
    ACTIVE("enum.block_mark_status.active"),
    EXPIRED("enum.block_mark_status.expired"),
    CONVERTED_TO_SALE("enum.block_mark_status.converted_to_sale"),
    RELEASED("enum.block_mark_status.released");

    private final String messageKey;

    BlockMarkStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
