package com.ozerler.marble.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class HelpFeedbackRequest {

    @NotNull
    @JsonProperty("helpful")
    private Boolean helpful;
}
