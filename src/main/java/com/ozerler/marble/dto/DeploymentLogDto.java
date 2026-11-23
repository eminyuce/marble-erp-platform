package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploymentLogDto {

    @JsonProperty("offset")
    private long offset;

    @JsonProperty("nextOffset")
    private long nextOffset;

    @JsonProperty("chunk")
    private String chunk;

    @JsonProperty("state")
    private DeploymentState state;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;
}
