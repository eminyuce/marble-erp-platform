package com.ozerler.marble.model.enums;

public enum BlockStatus {
    QUARRY("Ocak Sahasında"),
    IN_TRANSIT("Fabrika Yolunda (Nakliye)"),
    FACTORY_STOCK("Fabrika Hammadde Ambarı"),
    SAWING("Katraka Bağlandı (Kesimde)"),
    SOLD("Dış Müşteriye Satıldı"),
    SCRAPPED("Hurdaya / Moloza Ayrıldı");

    private final String label;

    BlockStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
