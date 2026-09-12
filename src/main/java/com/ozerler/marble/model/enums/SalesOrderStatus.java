package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum SalesOrderStatus {
    DRAFT("enum.sales_order_status.draft"),
    CONFIRMED("enum.sales_order_status.confirmed"),
    SHIPPED("enum.sales_order_status.shipped"),
    DELIVERED("enum.sales_order_status.delivered"),
    INVOICED("enum.sales_order_status.invoiced"),
    CANCELLED("enum.sales_order_status.cancelled");

    private final String messageKey;

    SalesOrderStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
