package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
@JsonPropertyOrder({"status", "dependencies"})
public class HealthResponse {

    @JsonProperty("status")
    String status;

    @JsonProperty("dependencies")
    Map<String, DependencyHealth> dependencies;
}
