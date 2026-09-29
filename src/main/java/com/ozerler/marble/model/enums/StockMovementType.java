package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum StockMovementType {
    INCOMING("enum.stock_movement_type.incoming", "Giriş"),
    OUTGOING("enum.stock_movement_type.outgoing", "Çıkış"),
    TRANSFER("enum.stock_movement_type.transfer", "Transfer"),
    PRODUCTION("enum.stock_movement_type.production", "Üretim"),
    PRODUCTION_CONSUMPTION("enum.stock_movement_type.production_consumption", "Üretim Tüketimi"),
    RESERVATION("enum.stock_movement_type.reservation", "Rezervasyon"),
    SCRAP("enum.stock_movement_type.scrap", "Fire"),
    RETURN("enum.stock_movement_type.return", "İade");

    private final String messageKey;
    private final String defaultLabel;

    StockMovementType(String messageKey, String defaultLabel) {
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
