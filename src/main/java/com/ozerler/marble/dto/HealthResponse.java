package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("dependencies")
    Map<String, DependencyHealth> dependencies;
}
