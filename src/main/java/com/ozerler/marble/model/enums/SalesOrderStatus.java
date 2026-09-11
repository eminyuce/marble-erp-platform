package com.ozerler.marble.model.enums;

public enum SalesOrderStatus {
    DRAFT("Taslak"),
    CONFIRMED("Onaylandı"),
    SHIPPED("Sevk Edildi"),
    DELIVERED("Teslim Edildi"),
    INVOICED("Faturalandı"),
    CANCELLED("İptal");

    private final String label;

    SalesOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
