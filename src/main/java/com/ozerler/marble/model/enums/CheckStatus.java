package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum CheckStatus {
    PORTFOLIO("enum.check_status.portfolio", "Portföyde"),
    COLLECTED("enum.check_status.collected", "Tahsil Edildi"),
    BOUNCED("enum.check_status.bounced", "Karşılıksız"),
    RETURNED("enum.check_status.returned", "İade Edildi");

    private final String messageKey;
    private final String defaultLabel;

    CheckStatus(String messageKey, String defaultLabel) {
        this.messageKey = messageKey;
        this.defaultLabel = defaultLabel;
    }

    public String getLabel() {
        try {
            return MessageUtils.getMessage(messageKey);
        } catch (Exception e) {
            return defaultLabel;
        }
    }

    public String getDisplayName() {
        return getLabel();
    }
}
