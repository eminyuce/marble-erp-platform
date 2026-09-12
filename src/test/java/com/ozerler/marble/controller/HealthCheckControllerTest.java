package com.ozerler.marble.controller;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.response.BackEndResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckControllerTest {

    private final HealthCheckController healthCheckController = new HealthCheckController();

    @Test
    @DisplayName("getHealth should return success BackEndResponse with health payload")
    void shouldReturnHealthCheckBackEndResponse() {
        BackEndResponse response = healthCheckController.getHealth();

        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Health check successful");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getResponse().getBody();
        assertThat(body).containsEntry("status", "UP");
        assertThat(body).containsEntry("service", "ozerler-marble-erp");
    }
}
