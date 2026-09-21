package com.ozerler.marble.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * One template variable shown on the email preview page with its sample value.
 */
@Getter
@Builder
public class EmailPlaceholderSample {
    private final String key;
    private final String sampleValue;
    private final boolean known;
}
