package com.ozerler.marble.model.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates status messages, error codes, and error descriptions.
 */
public class Status implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private List<String> errors;
    private String errorCode;

    public Status() {
    }

    public Status(String message, String errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String err) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        } else if (!(this.errors instanceof ArrayList)) {
            this.errors = new ArrayList<>(this.errors);
        }
        this.errors.add(err);
    }

    public void addAllErrors(List<String> errs) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        } else if (!(this.errors instanceof ArrayList)) {
            this.errors = new ArrayList<>(this.errors);
        }
        if (errs != null) {
            this.errors.addAll(errs);
        }
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Convenience method that checks if the error list contains any errors.
     *
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return this.errors != null && !this.errors.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Status other = (Status) obj;
        return Objects.equals(message, other.message) &&
                Objects.equals(errors, other.errors) &&
                Objects.equals(errorCode, other.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, errors, errorCode);
    }

    @Override
    public String toString() {
        return "Status [message=" + message + ", errorCode=" + errorCode + ", errors=" + errors + "]";
    }
}
