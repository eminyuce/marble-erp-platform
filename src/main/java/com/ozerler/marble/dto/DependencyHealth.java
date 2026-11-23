package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DependencyHealth {

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

    public static DependencyHealth up() {
        return DependencyHealth.builder().status("UP").build();
    }

    public static DependencyHealth down(String error) {
        return DependencyHealth.builder().status("DOWN").error(error).build();
    }
}
