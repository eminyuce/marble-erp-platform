package com.ozerler.marble.exception;

public class SlabNotFoundException extends ResourceNotFoundException {
    public SlabNotFoundException(Long id) {
        super("Plaka bulunamadı: " + id);
    }
}
