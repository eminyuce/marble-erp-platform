package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum TargetDestination {
    CUSTOMER("enum.target_destination.customer", "Müşteri"),
    FACTORY("enum.target_destination.factory", "Fabrika"),
    WORKSHOP("enum.target_destination.workshop", "Atölye"),
    SITE("enum.target_destination.site", "Şantiye"),
    OTHER("enum.target_destination.other", "Diğer");

    private final String messageKey;
    private final String defaultLabel;

    TargetDestination(String messageKey, String defaultLabel) {
        this.messageKey = messageKey;
        this.defaultLabel = defaultLabel;
    }

    public String getLabel() {
        try {
            String msg = MessageUtils.getMessage(messageKey);
            if (msg != null && !msg.equals(messageKey) && !msg.isBlank()) {
                return msg;
            }
        } catch (Exception ignored) {}
        return defaultLabel;
    }

    public String getDisplayName() {
        return getLabel();
    }
}
