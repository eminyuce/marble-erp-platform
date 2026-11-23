package com.ozerler.marble.exception;

public class ProductionOrderNotFoundException extends ResourceNotFoundException {
    public ProductionOrderNotFoundException(Long id) {
        super("Üretim emri bulunamadı: " + id);
    }
}
