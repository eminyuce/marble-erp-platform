package com.ozerler.marble.model.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackEndResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should correctly instantiate and access BackEndResponse fields")
    void shouldHandleBackEndResponseFields() {
        BackEndResponse ber = new BackEndResponse();
        ResponseEntity<String> responseEntity = ResponseEntity.ok("success-payload");
        ServiceStatus serviceStatus = new ServiceStatus();
        serviceStatus.setHttpStatus(HttpStatus.OK);

        Status status = new Status();
        status.setMessage("All good");
        status.setErrorCode("0");
        status.addError("detail error");
        status.addAllErrors(List.of("error2", "error3"));
        serviceStatus.setStatus(status);

        ber.setResponse(responseEntity);
        ber.setServiceStatus(serviceStatus);

        assertThat(ber.getResponse()).isEqualTo(responseEntity);
        assertThat(ber.getServiceStatus()).isEqualTo(serviceStatus);
        assertThat(ber.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(ber.getServiceStatus().getStatus().getMessage()).isEqualTo("All good");
        assertThat(ber.getServiceStatus().getStatus().getErrorCode()).isEqualTo("0");
        assertThat(ber.getServiceStatus().getStatus().hasErrors()).isTrue();
        assertThat(ber.getServiceStatus().getStatus().getErrors()).containsExactly("detail error", "error2", "error3");
        assertThat(ber.toString()).contains("BackEndResponse");
        assertThat(ber.hashCode()).isNotZero();

        BackEndResponse ber2 = new BackEndResponse(responseEntity, serviceStatus);
        assertThat(ber).isEqualTo(ber2);
    }

    @Test
    @DisplayName("Should serialize BackEndResponse to JSON with expected structure")
    void shouldSerializeToJson() throws Exception {
        Status status = new Status("Retrieval successful", "0");
        ServiceStatus serviceStatus = new ServiceStatus(HttpStatus.OK, status);
        BackEndResponse ber = new BackEndResponse(ResponseEntity.ok("test-data"), serviceStatus);

        String json = objectMapper.writeValueAsString(ber);

        assertThat(json).contains("\"serviceStatus\"");
        assertThat(json).contains("\"httpStatus\":\"OK\"");
        assertThat(json).contains("\"message\":\"Retrieval successful\"");
        assertThat(json).contains("\"errorCode\":\"0\"");
        assertThat(json).contains("\"response\"");
    }

    @Test
    @DisplayName("Should handle empty error list correctly in Status")
    void shouldHandleEmptyErrorsInStatus() {
        Status status = new Status();
        assertThat(status.hasErrors()).isFalse();

        status.setErrors(List.of());
        assertThat(status.hasErrors()).isFalse();

        status.addError("err");
        assertThat(status.hasErrors()).isTrue();
    }
}
