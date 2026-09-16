package com.ozerler.marble.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StoredFileNotFoundException extends ResourceNotFoundException {

    public StoredFileNotFoundException(String message) {
        super(message);
    }

    public StoredFileNotFoundException(Object id) {
        super("Dosya", id);
    }
}
