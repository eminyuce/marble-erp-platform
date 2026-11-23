package com.ozerler.marble.exception;

public class SalesOrderNotFoundException extends ResourceNotFoundException {
    public SalesOrderNotFoundException(Long id) {
        super("Satış siparişi bulunamadı: " + id);
    }
}
