package com.ozerler.marble.exception;

public class CutOrderNotFoundException extends ResourceNotFoundException {
    public CutOrderNotFoundException(Long id) {
        super("Atölye iş emri bulunamadı: " + id);
    }
}
