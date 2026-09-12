package com.ozerler.marble.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Standard backend response envelope wrapping an inner ResponseEntity payload
 * and service execution status information.
 */
public class BackEndResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("response")
    private ResponseEntity<?> response;

    @JsonProperty("serviceStatus")
    private ServiceStatus serviceStatus;

    public BackEndResponse() {
    }

    public BackEndResponse(ResponseEntity<?> response, ServiceStatus serviceStatus) {
        this.response = response;
        this.serviceStatus = serviceStatus;
    }

    public ResponseEntity<?> getResponse() {
        return response;
    }

    public void setResponse(ResponseEntity<?> response) {
        this.response = response;
    }

    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BackEndResponse other = (BackEndResponse) obj;
        return Objects.equals(response, other.response) &&
                Objects.equals(serviceStatus, other.serviceStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(response, serviceStatus);
    }

    @Override
    public String toString() {
        return "BackEndResponse [response=" + response + ", serviceStatus=" + serviceStatus + "]";
    }
}
