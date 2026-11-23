package com.ozerler.marble.exception;

public class PurchaseOrderNotFoundException extends ResourceNotFoundException {
    public PurchaseOrderNotFoundException(Long id) {
        super("Satınalma siparişi bulunamadı: " + id);
    }
}
