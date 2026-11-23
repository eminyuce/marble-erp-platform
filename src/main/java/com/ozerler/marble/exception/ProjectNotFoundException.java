package com.ozerler.marble.exception;

public class ProjectNotFoundException extends ResourceNotFoundException {
    public ProjectNotFoundException(Long id) {
        super("Proje bulunamadı: " + id);
    }
}
