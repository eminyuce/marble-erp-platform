package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DeploymentStatusDto {

    @JsonProperty("state")
    DeploymentState state;

    @JsonProperty("canStart")
    boolean canStart;

    @JsonProperty("linuxHost")
    boolean linuxHost;

    @JsonProperty("enabled")
    boolean enabled;

    @JsonProperty("commandAvailable")
    boolean commandAvailable;

    @JsonProperty("sudoReady")
    boolean sudoReady;

    @JsonProperty("branch")
    String branch;

    @JsonProperty("headCommit")
    String headCommit;

    @JsonProperty("headMessage")
    String headMessage;

    @JsonProperty("startedAt")
    String startedAt;

    @JsonProperty("finishedAt")
    String finishedAt;

    @JsonProperty("message")
    String message;

    @JsonProperty("blockingReasons")
    List<String> blockingReasons;
}
