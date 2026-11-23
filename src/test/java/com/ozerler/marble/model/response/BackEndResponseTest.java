package com.ozerler.marble.model.response;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

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

        Assertions.assertThat(ber.getResponse()).isEqualTo(responseEntity);
        Assertions.assertThat(ber.getServiceStatus()).isEqualTo(serviceStatus);
        Assertions.assertThat(ber.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(ber.getServiceStatus().getStatus().getMessage()).isEqualTo("All good");
        Assertions.assertThat(ber.getServiceStatus().getStatus().getErrorCode()).isEqualTo("0");
        Assertions.assertThat(ber.getServiceStatus().getStatus().hasErrors()).isTrue();
        Assertions.assertThat(ber.getServiceStatus().getStatus().getErrors()).containsExactly("detail error", "error2", "error3");
        Assertions.assertThat(ber.toString()).contains("BackEndResponse");
        Assertions.assertThat(ber.hashCode()).isNotZero();

        BackEndResponse ber2 = new BackEndResponse(responseEntity, serviceStatus);
        Assertions.assertThat(ber).isEqualTo(ber2);
    }

    @Test
    @DisplayName("Should serialize BackEndResponse to JSON with expected structure")
    void shouldSerializeToJson() throws Exception {
        Status status = new Status("Retrieval successful", "0");
        ServiceStatus serviceStatus = new ServiceStatus(HttpStatus.OK, status);
        BackEndResponse ber = new BackEndResponse(ResponseEntity.ok("test-data"), serviceStatus);

        String json = objectMapper.writeValueAsString(ber);

        Assertions.assertThat(json).contains("\"serviceStatus\"");
        Assertions.assertThat(json).contains("\"httpStatus\":\"OK\"");
        Assertions.assertThat(json).contains("\"message\":\"Retrieval successful\"");
        Assertions.assertThat(json).contains("\"errorCode\":\"0\"");
        Assertions.assertThat(json).contains("\"response\"");
    }

    @Test
    @DisplayName("Should handle empty error list correctly in Status")
    void shouldHandleEmptyErrorsInStatus() {
        Status status = new Status();
        Assertions.assertThat(status.hasErrors()).isFalse();

        status.setErrors(List.of());
        Assertions.assertThat(status.hasErrors()).isFalse();

        status.addError("err");
        Assertions.assertThat(status.hasErrors()).isTrue();
    }
}
