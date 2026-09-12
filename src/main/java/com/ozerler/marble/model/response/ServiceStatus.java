package com.ozerler.marble.model.response;

import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates the HTTP status and application-level status details.
 */
public class ServiceStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private HttpStatus httpStatus;
    private Status status;

    public ServiceStatus() {
    }

    public ServiceStatus(HttpStatus httpStatus, Status status) {
        this.httpStatus = httpStatus;
        this.status = status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ServiceStatus other = (ServiceStatus) obj;
        return httpStatus == other.httpStatus && Objects.equals(status, other.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpStatus, status);
    }

    @Override
    public String toString() {
        return "ServiceStatus [httpStatus=" + httpStatus + ", status=" + status + "]";
    }
}
