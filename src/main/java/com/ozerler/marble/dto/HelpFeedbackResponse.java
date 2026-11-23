package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelpFeedbackResponse {

    @JsonProperty("recorded")
    private boolean recorded;

    @JsonProperty("helpful")
    private boolean helpful;

    @JsonProperty("pageKey")
    private String pageKey;
}
