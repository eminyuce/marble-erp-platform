package com.ozerler.marble.model.enums;

public enum ExpenseType {
    DIRECT_RAW("Hammadde (Blok)"),
    DIRECT_LABOR("Direkt İşçilik"),
    ELECTRICITY("Elektrik / Enerji"),
    CONSUMABLES("Sarf Malzemesi (Lama/Disk/Aşındırıcı)"),
    LOGISTICS("Nakliye & Akaryakıt"),
    DEPRECIATION("Makine Amortismanı"),
    OVERHEAD("Genel Üretim Gideri (GÜG)");

    private final String label;

    ExpenseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
