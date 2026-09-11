package com.ozerler.marble.model.enums;

public enum ProjectStatus {
    PLANNED("Planlama Aşamasında"),
    ACTIVE("Aktif İmalat & Montaj"),
    ON_HOLD("Beklemede / Askıda"),
    COMPLETED("Teslim Edildi / Tamamlandı");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
