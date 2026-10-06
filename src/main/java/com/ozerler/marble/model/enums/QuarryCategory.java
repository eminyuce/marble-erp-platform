package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

/**
 * Ocak modülünde fatura ve stok kartları için 4 sabit ana kategori.
 * Bu kategoriler değiştirilemez, silinemez ve yeniden adlandırılamaz.
 */
public enum QuarryCategory {
    MAZOT("enum.quarry_category.mazot", "Mazot", "Mazot alımı ve mazot stoğu"),
    SARF_MALZEME("enum.quarry_category.sarf_malzeme", "Sarf Malzeme", "Ocakta kullanılan sarf malzemeleri (yağ, filtre, kesici ekipman vb.)"),
    ELEKTRIK("enum.quarry_category.elektrik", "Elektrik", "Elektrik faturaları ve enerji giderleri"),
    DIGER("enum.quarry_category.diger", "Diğer", "Yukarıdaki kategorilere girmeyen ürün ve giderler");

    private final String messageKey;
    private final String defaultLabel;
    private final String description;

    QuarryCategory(String messageKey, String defaultLabel, String description) {
        this.messageKey = messageKey;
        this.defaultLabel = defaultLabel;
        this.description = description;
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

    public String getDescription() {
        return description;
    }
}
