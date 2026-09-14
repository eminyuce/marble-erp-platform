package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HelpFeedbackRequest {

    @NotNull
    @JsonProperty("helpful")
    private Boolean helpful;
}
