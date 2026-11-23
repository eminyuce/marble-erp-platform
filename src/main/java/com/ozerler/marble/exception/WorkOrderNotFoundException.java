package com.ozerler.marble.exception;

public class WorkOrderNotFoundException extends ResourceNotFoundException {
    public WorkOrderNotFoundException(Long id) {
        super("İş emri bulunamadı: " + id);
    }
}
