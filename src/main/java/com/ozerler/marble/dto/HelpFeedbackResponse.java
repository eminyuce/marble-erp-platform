package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelpFeedbackResponse {

    @JsonProperty("recorded")
    boolean recorded;

    @JsonProperty("helpful")
    boolean helpful;

    @JsonProperty("pageKey")
    String pageKey;
}
