package com.ozerler.marble.exception;

public class InvoiceNotFoundException extends ResourceNotFoundException {
    public InvoiceNotFoundException(Long id) {
        super("Fatura bulunamadı: " + id);
    }
}
