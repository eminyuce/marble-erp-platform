package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum OperationWorkOrderStatus {
    NEW("enum.work_order_status.new", "Yeni"),
    APPROVED("enum.work_order_status.approved", "Onaylandı"),
    IN_PRODUCTION("enum.work_order_status.in_production", "Üretimde"),
    ON_HOLD("enum.work_order_status.on_hold", "Beklemede"),
    COMPLETED("enum.work_order_status.completed", "Tamamlandı"),
    DISPATCHED("enum.work_order_status.dispatched", "Sevk Edildi"),
    CANCELLED("enum.work_order_status.cancelled", "İptal");

    private final String messageKey;
    private final String defaultLabel;

    OperationWorkOrderStatus(String messageKey, String defaultLabel) {
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
