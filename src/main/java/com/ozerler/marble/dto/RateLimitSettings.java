package com.ozerler.marble.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Effective rate-limit configuration after applying database overrides
 * on top of the YAML defaults.
 */
@Getter
@EqualsAndHashCode
public final class RateLimitSettings {

    private final boolean enabled;
    private final int loginMaxRequests;
    private final int loginWindowSeconds;
    private final int formMaxRequests;
    private final int formWindowSeconds;
    private final int generalMaxRequests;
    private final int generalWindowSeconds;

    public RateLimitSettings(boolean enabled,
                             int loginMaxRequests,
                             int loginWindowSeconds,
                             int formMaxRequests,
                             int formWindowSeconds,
                             int generalMaxRequests,
                             int generalWindowSeconds) {
        this.enabled = enabled;
        this.loginMaxRequests = loginMaxRequests;
        this.loginWindowSeconds = loginWindowSeconds;
        this.formMaxRequests = formMaxRequests;
        this.formWindowSeconds = formWindowSeconds;
        this.generalMaxRequests = generalMaxRequests;
        this.generalWindowSeconds = generalWindowSeconds;
    }
}
