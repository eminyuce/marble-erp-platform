package com.ozerler.marble.model.enums;

public enum ProcessType {
    GANGSAW("Katrak Dilimleme (80 Lama)"),
    ST("Dikey Yarma (ST)"),
    POLISHING("Cila Hattı (Epoksi & File)"),
    HONING("Honlama"),
    RESIN_LINE("Reçine / Fırın"),
    BRIDGE_CUTTING("Köprü Kesme");

    private final String label;

    ProcessType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
