package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum InvoiceStatus {
    DRAFT("enum.invoice_status.draft", "Taslak"),
    ISSUED("enum.invoice_status.issued", "Onaylandı / Kesildi"),
    PAID("enum.invoice_status.paid", "Ödendi / Tahsil Edildi"),
    PARTIALLY_PAID("enum.invoice_status.partially_paid", "Kısmi Tahsil Edildi"),
    CANCELLED("enum.invoice_status.cancelled", "İptal Edildi");

    private final String messageKey;
    private final String defaultLabel;

    InvoiceStatus(String messageKey, String defaultLabel) {
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
