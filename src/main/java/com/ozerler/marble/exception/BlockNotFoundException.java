package com.ozerler.marble.exception;

public class BlockNotFoundException extends ResourceNotFoundException {
    public BlockNotFoundException(Long id) {
        super("Blok bulunamadı: " + id);
    }
}
