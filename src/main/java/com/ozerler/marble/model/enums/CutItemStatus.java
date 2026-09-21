package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

/**
 * Lifecycle of fabricated cut items stored as string codes on {@code CutItem}.
 */
public enum CutItemStatus {
    READY("enum.cut_item_status.ready"),
    PACKED("enum.cut_item_status.packed"),
    DELIVERED("enum.cut_item_status.delivered"),
    INSTALLED("enum.cut_item_status.installed");

    private final String messageKey;

    CutItemStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public static String labelOf(String code) {
        return NamedEnumLabels.labelOf(CutItemStatus.class, code, status -> status.getLabel());
    }
}
