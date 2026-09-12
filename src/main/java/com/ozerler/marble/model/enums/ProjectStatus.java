package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ProjectStatus {
    PLANNED("enum.project_status.planned"),
    ACTIVE("enum.project_status.active"),
    ON_HOLD("enum.project_status.on_hold"),
    COMPLETED("enum.project_status.completed");

    private final String messageKey;

    ProjectStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
