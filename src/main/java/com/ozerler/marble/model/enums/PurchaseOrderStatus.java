package com.ozerler.marble.model.enums;

public enum PurchaseOrderStatus {
    DRAFT("Taslak"),
    SENT("Gönderildi"),
    CONFIRMED("Onaylandı"),
    PARTIAL_DELIVERY("Kısmi Teslimat"),
    DELIVERED("Teslim Alındı"),
    CANCELLED("İptal");

    private final String label;

    PurchaseOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
