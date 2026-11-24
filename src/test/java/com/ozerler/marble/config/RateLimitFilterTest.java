package com.ozerler.marble.config;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.ozerler.marble.dto.RateLimitSettings;
import com.ozerler.marble.service.RateLimitService;
import com.ozerler.marble.service.RateLimitSettingsService;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitSettingsService rateLimitSettingsService;

    @Mock
    private MessageSource messageSource;

    private final AtomicReference<RateLimitSettings> current = new AtomicReference<>();

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        current.set(settings(true, 5, 60, 10, 60, 1, 60));
        Mockito.when(rateLimitSettingsService.effective()).thenAnswer(invocation -> current.get());
        Mockito.lenient().when(messageSource.getMessage(
                        Mockito.anyString(), Mockito.isNull(), Mockito.any(Locale.class)))
                .thenReturn("limited");
        RateLimitService rateLimitService = new RateLimitService(rateLimitSettingsService);
        filter = new RateLimitFilter(rateLimitService, rateLimitSettingsService, messageSource);
    }

    @Test
    @DisplayName("requests pass through when rate limiting is disabled")
    void disabledSkipsLimiting() throws Exception {
        current.set(settings(false, 1, 60, 1, 60, 1, 60));
        MockHttpServletResponse first = performGet("/blocks");
        MockHttpServletResponse second = performGet("/blocks");
        Assertions.assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
        Assertions.assertThat(second.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("the filter enforces the effective general limit and picks up a tighter override")
    void enforcesLimitAndPicksUpNewValues() throws Exception {
        MockHttpServletResponse first = performGet("/blocks");
        MockHttpServletResponse second = performGet("/blocks");
        Assertions.assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
        Assertions.assertThat(second.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        Assertions.assertThat(second.getHeader("Retry-After")).isEqualTo("60");

        current.set(settings(true, 5, 60, 10, 60, 2, 30));
        MockHttpServletResponse afterChange = performGet("/blocks");
        Assertions.assertThat(afterChange.getStatus())
                .as("resized buckets should allow a request under the new limit")
                .isEqualTo(HttpStatus.OK.value());
        Assertions.assertThat(performGet("/blocks").getStatus()).isEqualTo(HttpStatus.OK.value());
        MockHttpServletResponse exceeded = performGet("/blocks");
        Assertions.assertThat(exceeded.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        Assertions.assertThat(exceeded.getHeader("Retry-After")).isEqualTo("30");
    }

    @Test
    @DisplayName("loopback and static brand assets are not rate limited")
    void loopbackAndStaticAssetsBypass() throws Exception {
        current.set(settings(true, 1, 60, 1, 60, 1, 60));
        MockHttpServletRequest loopback = new MockHttpServletRequest("GET", "/blocks");
        loopback.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse loopbackResponse = new MockHttpServletResponse();
        filter.doFilter(loopback, loopbackResponse, new MockFilterChain());
        filter.doFilter(loopback, loopbackResponse, new MockFilterChain());
        Assertions.assertThat(loopbackResponse.getStatus()).isEqualTo(HttpStatus.OK.value());

        MockHttpServletResponse icon = performGet("/icons/android-chrome-48x48.png");
        MockHttpServletResponse manifest = performGet("/site.webmanifest");
        MockHttpServletResponse robots = performGet("/robots.txt");
        Assertions.assertThat(icon.getStatus()).isEqualTo(HttpStatus.OK.value());
        Assertions.assertThat(manifest.getStatus()).isEqualTo(HttpStatus.OK.value());
        Assertions.assertThat(robots.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    private MockHttpServletResponse performGet(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
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
