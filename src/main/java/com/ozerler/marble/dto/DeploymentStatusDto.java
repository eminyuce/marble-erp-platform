package com.ozerler.marble.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploymentStatusDto {

    @JsonProperty("state")
    private DeploymentState state;

    @JsonProperty("canStart")
    private boolean canStart;

    @JsonProperty("linuxHost")
    private boolean linuxHost;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("commandAvailable")
    private boolean commandAvailable;

    @JsonProperty("sudoReady")
    private boolean sudoReady;

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("headCommit")
    private String headCommit;

    @JsonProperty("headMessage")
    private String headMessage;

    @JsonProperty("imageRepository")
    private String imageRepository;

    @JsonProperty("imageTag")
    private String imageTag;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("message")
    private String message;

    @JsonProperty("blockingReasons")
    private List<String> blockingReasons;
}
