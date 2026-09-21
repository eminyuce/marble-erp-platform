package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ShipmentDeliveryStatus {
    PREPARING("enum.shipment_delivery_status.preparing"),
    IN_TRANSIT("enum.shipment_delivery_status.in_transit"),
    DELIVERED("enum.shipment_delivery_status.delivered"),
    CANCELLED("enum.shipment_delivery_status.cancelled");

    private final String messageKey;

    ShipmentDeliveryStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public static String labelOf(String code) {
        return NamedEnumLabels.labelOf(ShipmentDeliveryStatus.class, code, status -> status.getLabel());
    }
}
