package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum SlabStatus {
    AVAILABLE("enum.slab_status.available"),
    RESERVED("enum.slab_status.reserved"),
    IN_CUTTING("enum.slab_status.in_cutting"),
    SCRAPPED("enum.slab_status.scrapped"),
    INSTALLED("enum.slab_status.installed");

    private final String messageKey;

    SlabStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
