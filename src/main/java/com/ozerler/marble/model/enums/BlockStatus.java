package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum BlockStatus {
    QUARRY("enum.block_status.quarry"),
    IN_TRANSIT("enum.block_status.in_transit"),
    FACTORY_STOCK("enum.block_status.factory_stock"),
    SAWING("enum.block_status.sawing"),
    SOLD("enum.block_status.sold"),
    SCRAPPED("enum.block_status.scrapped");

    private final String messageKey;

    BlockStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
