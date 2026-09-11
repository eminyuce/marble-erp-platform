package com.ozerler.marble.model.enums;

public enum SurfaceFinish {
    RAW("Ham (Testere Çıkışı)"),
    POLISHED("Cilalı (Parlak)"),
    HONED("Honlu (Mat)"),
    BRUSHED("Fırçalı (Patinato)"),
    FLAMED("Yakılmış (Alevli)"),
    BUSH_HAMMERED("Taraklı / Çekiçlenmiş");

    private final String label;

    SurfaceFinish(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
