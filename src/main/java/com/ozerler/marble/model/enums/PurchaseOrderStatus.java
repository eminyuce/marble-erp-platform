package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum PurchaseOrderStatus {
    DRAFT("enum.purchase_order_status.draft"),
    SENT("enum.purchase_order_status.sent"),
    CONFIRMED("enum.purchase_order_status.confirmed"),
    PARTIAL_DELIVERY("enum.purchase_order_status.partial_delivery"),
    DELIVERED("enum.purchase_order_status.delivered"),
    CANCELLED("enum.purchase_order_status.cancelled");

    private final String messageKey;

    PurchaseOrderStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
