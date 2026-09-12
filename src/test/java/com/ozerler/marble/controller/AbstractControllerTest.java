package com.ozerler.marble.controller;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractControllerTest {

    private static class TestController extends AbstractController {
    }

    private final TestController controller = new TestController();

    @Test
    @DisplayName("Should build fatal response with defaults")
    void shouldBuildFatalResponse() {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();

        BackEndResponse result = controller.buildFatalResponse(ber, serviceStatus, status, "testAction", "3399");

        assertThat(result.getServiceStatus()).isNotNull();
        assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo("3399");
        assertThat(result.getServiceStatus().getStatus().getMessage()).isEqualTo("A serious error occurred in testAction");
        assertThat(result.getServiceStatus().getStatus().getErrors()).contains("Request failed");
    }

    @Test
    @DisplayName("Should build fatal response with null arguments gracefully")
    void shouldBuildFatalResponseWithNulls() {
        BackEndResponse result = controller.buildFatalResponse(null, null, null, "nullAction", null);

        assertThat(result).isNotNull();
        assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
    }

    @Test
    @DisplayName("Should build failure response with custom status")
    void shouldBuildFailureResponse() {
        BackEndResponse result = controller.buildFailureResponse(null, null, null, "customAction", "4004", HttpStatus.NOT_FOUND);

        assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo("4004");
    }

    @Test
    @DisplayName("Should build success response correctly")
    void shouldBuildSuccessResponse() {
        BackEndResponse result = controller.buildSuccessResponse("data-payload", "Success message");

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getBody()).isEqualTo("data-payload");
        assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(result.getServiceStatus().getStatus().getMessage()).isEqualTo("Success message");
    }
}
