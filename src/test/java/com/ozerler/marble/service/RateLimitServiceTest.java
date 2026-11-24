package com.ozerler.marble.service;

import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.dto.RateLimitSettings;
import com.ozerler.marble.service.RateLimitService.Tier;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimitSettingsService rateLimitSettingsService;

    private final AtomicReference<RateLimitSettings> current = new AtomicReference<>();

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        current.set(settings(true, 2, 60, 10, 60, 5, 60));
        Mockito.when(rateLimitSettingsService.effective()).thenAnswer(invocation -> current.get());
        rateLimitService = new RateLimitService(rateLimitSettingsService);
    }

    @Test
    @DisplayName("isAllowed denies a client once the current tier max is reached")
    void deniesWhenMaxReached() {
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.LOGIN)).isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.LOGIN)).isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.LOGIN)).isFalse();
        Assertions.assertThat(rateLimitService.isAllowed("client-b", Tier.LOGIN)).isTrue();
    }

    @Test
    @DisplayName("a settings change is picked up immediately and existing windows are cleared")
    void picksUpNewValuesAndClearsWindows() {
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL)).isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL)).isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL)).isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL)).isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL)).isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL)).isFalse();

        current.set(settings(true, 2, 60, 10, 60, 1, 60));

        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL))
                .as("resized buckets should start empty under the new limit")
                .isTrue();
        Assertions.assertThat(rateLimitService.isAllowed("client-a", Tier.GENERAL)).isFalse();
        Assertions.assertThat(rateLimitService.windowSeconds(Tier.GENERAL)).isEqualTo(60);
    }

    private static RateLimitSettings settings(boolean enabled,
                                              int loginMax,
                                              int loginWindow,
                                              int formMax,
                                              int formWindow,
                                              int generalMax,
                                              int generalWindow) {
        return new RateLimitSettings(
                enabled, loginMax, loginWindow, formMax, formWindow, generalMax, generalWindow);
    }
}
