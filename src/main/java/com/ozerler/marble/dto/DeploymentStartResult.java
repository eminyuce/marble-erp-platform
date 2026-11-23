package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploymentStartResult {

    @JsonProperty("accepted")
    private boolean accepted;

    @JsonProperty("state")
    private DeploymentState state;

    @JsonProperty("message")
    private String message;
}
