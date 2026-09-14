package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

/**
 * Lifecycle of production orders and cut orders stored as string codes on those
 * entities.
 */
public enum OperationStatus {
    PLANNED("enum.operation_status.planned"),
    IN_PROGRESS("enum.operation_status.in_progress"),
    COMPLETED("enum.operation_status.completed"),
    CANCELLED("enum.operation_status.cancelled");

    private final String messageKey;

    OperationStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public static String labelOf(String code) {
        return NamedEnumLabels.labelOf(OperationStatus.class, code, OperationStatus::getLabel);
    }
}
