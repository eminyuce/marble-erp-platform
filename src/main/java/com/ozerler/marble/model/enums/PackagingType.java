package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum PackagingType {
    A_FRAME("enum.packaging_type.a_frame"),
    EXPORT_CRATE("enum.packaging_type.export_crate"),
    WOOD_BUNDLE("enum.packaging_type.wood_bundle"),
    BUNDLE("enum.packaging_type.bundle"),
    LOOSE("enum.packaging_type.loose");

    private final String messageKey;

    PackagingType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
