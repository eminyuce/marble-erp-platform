package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum FactoryWorkOrderStatus {
    ACCEPTED("enum.factory_work_order_status.accepted"),
    ASSIGNED("enum.factory_work_order_status.assigned"),
    IN_PROGRESS("enum.factory_work_order_status.in_progress"),
    WAITING_NEXT_STEP("enum.factory_work_order_status.waiting_next_step"),
    COMPLETED("enum.factory_work_order_status.completed"),
    CANCELLED("enum.factory_work_order_status.cancelled");

    private final String messageKey;

    FactoryWorkOrderStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
