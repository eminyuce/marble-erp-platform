package com.ozerler.marble.model.enums;

public enum CustomerType {
    MARBLE_APPLICATION("Mermer Uygulama"),
    CONSTRUCTION("İnşaat Firması"),
    DEALER("Bayi/Toptancı");

    private final String label;

    CustomerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
