package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploymentLogDto {

    @JsonProperty("offset")
    long offset;

    @JsonProperty("nextOffset")
    long nextOffset;

    @JsonProperty("chunk")
    String chunk;

    @JsonProperty("state")
    DeploymentState state;

    @JsonProperty("startedAt")
    String startedAt;

    @JsonProperty("finishedAt")
    String finishedAt;
}
