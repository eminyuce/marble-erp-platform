package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum StockProductType {
    BLOCK("enum.stock_product_type.block", "Blok"),
    SLAB("enum.stock_product_type.slab", "Plaka"),
    SIZED("enum.stock_product_type.sized", "Ebatlı"),
    FUEL("enum.stock_product_type.fuel", "Mazot"),
    CONSUMABLE("enum.stock_product_type.consumable", "Sarf Malzeme"),
    OTHER("enum.stock_product_type.other", "Diğer");

    private final String messageKey;
    private final String defaultLabel;

    StockProductType(String messageKey, String defaultLabel) {
        this.messageKey = messageKey;
        this.defaultLabel = defaultLabel;
    }

    public String getLabel() {
        try {
            String msg = MessageUtils.getMessage(messageKey);
            if (msg != null && !msg.equals(messageKey) && !msg.isBlank()) {
                return msg;
            }
        } catch (Exception ignored) {
        }
        return defaultLabel;
    }

    public String getDisplayName() {
        return getLabel();
    }
}
