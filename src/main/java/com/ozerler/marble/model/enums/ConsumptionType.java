package com.ozerler.marble.model.enums;

public enum ConsumptionType {
    STONE("Doğal Taş Plaka/Ebatlı"),
    ADHESIVE("Granit/Mermer Yapıştırıcısı"),
    GROUT("Derz Dolgusu"),
    CHEMICAL("Epoksi/Sarf Kimyasalı"),
    LABOR("Montaj İşçilik Puantajı");

    private final String label;

    ConsumptionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
