package com.ozerler.marble.service;

/**
 * A user-facing two-factor error. The message is a message-bundle code, never a secret.
 */
public class TwoFactorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TwoFactorException(String messageCode) {
        super(messageCode);
    }

    public String getMessageCode() {
        return getMessage();
    }
}
