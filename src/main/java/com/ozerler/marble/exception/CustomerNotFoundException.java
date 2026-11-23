package com.ozerler.marble.exception;

public class CustomerNotFoundException extends ResourceNotFoundException {
    public CustomerNotFoundException(Long id) {
        super("Cari kart bulunamadı: " + id);
    }
}
