package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum InvoiceType {
    PURCHASE("enum.invoice_type.purchase", "Alış Faturası"),
    SALES("enum.invoice_type.sales", "Satış Faturası");

    private final String messageKey;
    private final String defaultLabel;

    InvoiceType(String messageKey, String defaultLabel) {
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
