package com.ozerler.marble.model.enums;

public enum PackagingType {
    A_FRAME("A-Frame Çelik Sehpa"),
    EXPORT_CRATE("İhracat Tipi Ahşap Sandık"),
    WOOD_BUNDLE("Ahşap Palet / Takoz Bağ"),
    BUNDLE("Ahşap Palet / Takoz Bağ"),
    LOOSE("Dökme / Sehpasız");

    private final String label;

    PackagingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
