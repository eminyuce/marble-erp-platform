package com.ozerler.marble.model.enums;

public enum PurchaseItemType {
    CONSUMABLE("Sarf Malzeme"),
    ADHESIVE("Yapıştırıcı"),
    GROUT("Derz Dolgusu"),
    CHEMICAL("Kimyasal"),
    STONE("Doğal Taş Plaka"),
    EQUIPMENT("Ekipman/Alet"),
    SAND("Kum"),
    CEMENT("Çimento");

    private final String label;

    PurchaseItemType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
