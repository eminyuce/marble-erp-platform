package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum SupplyRoute {
    INTERNAL_PRODUCTION("enum.supply_route.internal_production"),
    EXTERNAL_PURCHASE("enum.supply_route.external_purchase"),
    MIXED("enum.supply_route.mixed");

    private final String messageKey;

    SupplyRoute(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
