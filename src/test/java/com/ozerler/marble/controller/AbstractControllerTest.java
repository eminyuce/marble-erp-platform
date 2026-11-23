package com.ozerler.marble.controller;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;

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

        Assertions.assertThat(result.getServiceStatus()).isNotNull();
        Assertions.assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo("3399");
        Assertions.assertThat(result.getServiceStatus().getStatus().getMessage())
                .isEqualTo("A serious error occurred in testAction");
        Assertions.assertThat(result.getServiceStatus().getStatus().getErrors()).contains("Request failed");
    }

    @Test
    @DisplayName("Should build fatal response with null arguments gracefully")
    void shouldBuildFatalResponseWithNulls() {
        BackEndResponse result = controller.buildFatalResponse(null, null, null, "nullAction", null);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
    }

    @Test
    @DisplayName("Should build failure response with custom status")
    void shouldBuildFailureResponse() {
        BackEndResponse result =
                controller.buildFailureResponse(
                        null, null, null, "customAction", "4004", HttpStatus.NOT_FOUND);

        Assertions.assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo("4004");
    }

    @Test
    @DisplayName("Should build success response correctly")
    void shouldBuildSuccessResponse() {
        BackEndResponse result = controller.buildSuccessResponse("data-payload", "Success message");

        Assertions.assertThat(result.getResponse()).isNotNull();
        Assertions.assertThat(result.getResponse().getBody()).isEqualTo("data-payload");
        Assertions.assertThat(result.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(result.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(result.getServiceStatus().getStatus().getMessage()).isEqualTo("Success message");
    }
}
