package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploymentStartResult {

    @JsonProperty("accepted")
    boolean accepted;

    @JsonProperty("state")
    DeploymentState state;

    @JsonProperty("message")
    String message;
}
