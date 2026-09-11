package com.ozerler.marble.model.enums;

public enum SupplierType {
    CONSUMABLE("Sarf Malzeme"),
    CHEMICAL("Yapı Kimyasalı"),
    STONE("Doğal Taş"),
    EQUIPMENT("Ekipman/Alet");

    private final String label;

    SupplierType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
