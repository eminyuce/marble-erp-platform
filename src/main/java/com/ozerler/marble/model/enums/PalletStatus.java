package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum PalletStatus {
    PREPARING("enum.pallet_status.preparing"),
    READY("enum.pallet_status.ready"),
    LOADED("enum.pallet_status.loaded"),
    SHIPPED("enum.pallet_status.shipped"),
    DELIVERED("enum.pallet_status.delivered");

    private final String messageKey;

    PalletStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public static String labelOf(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        String normalized = switch (code.trim().toUpperCase()) {
            case "OPEN" -> PREPARING.name();
            case "PACKED" -> READY.name();
            default -> code.trim();
        };
        return NamedEnumLabels.labelOf(PalletStatus.class, normalized, PalletStatus::getLabel);
    }
}
