package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ChamferStatus {
    NOT_APPLICABLE("enum.chamfer_status.not_applicable"),
    CHAMFERED("enum.chamfer_status.chamfered"),
    UNCHAMFERED("enum.chamfer_status.unchamfered");

    private final String messageKey;

    ChamferStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
