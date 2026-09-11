package com.ozerler.marble.model.enums;

public enum SlabStatus {
    AVAILABLE("Serbest Stok"),
    RESERVED("Proje/Siparişe Rezerve"),
    IN_CUTTING("Atölyede Kesimde"),
    SCRAPPED("Fire / Kırık"),
    INSTALLED("Şantiyede Monte Edildi");

    private final String label;

    SlabStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
