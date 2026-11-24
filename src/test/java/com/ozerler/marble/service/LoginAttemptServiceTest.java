package com.ozerler.marble.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.config.TwoFactorProperties;
import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.UserRepository;

class LoginAttemptServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private UserRepository userRepository;
    private LoginAttemptService service;
    private MutableClock clock;
    private final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new LoginAttemptService(userRepository, new TwoFactorProperties());
        clock = new MutableClock(Instant.parse("2026-11-24T07:00:00Z"));
        service.setClock(clock);
        User user = User.builder().email("ali@ozerler.com").username("ali").build();
        user.setId(7L);
        when(userRepository.findByEmail("ali@ozerler.com")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nobody@ozerler.com")).thenReturn(Optional.empty());
        when(userRepository.incrementFailedLoginAttempts(7L)).thenAnswer(inv -> {
            counter.incrementAndGet();
            return 1;
        });
        when(userRepository.findFailedLoginAttempts(7L)).thenAnswer(inv -> counter.get());
    }

    @Test
    @DisplayName("Five failures from one IP inside a minute block that IP only")
    void ipBlockedAfterFive() {
        for (int i = 0; i < 4; i++) {
            service.recordIpFailure("10.0.0.1");
        }
        assertThat(service.isIpBlocked("10.0.0.1")).isFalse();
        service.recordIpFailure("10.0.0.1");
        assertThat(service.isIpBlocked("10.0.0.1")).isTrue();
        assertThat(service.isIpBlocked("10.0.0.2")).isFalse();
    }

    @Test
    @DisplayName("IP failures older than the window no longer count")
    void ipWindowSlides() {
        for (int i = 0; i < 5; i++) {
            service.recordIpFailure("10.0.0.3");
        }
        assertThat(service.isIpBlocked("10.0.0.3")).isTrue();
        clock.advanceSeconds(61);
        assertThat(service.isIpBlocked("10.0.0.3")).isFalse();
    }

    @Test
    @DisplayName("Fifth consecutive wrong password locks the account for 15 minutes")
    void accountLocksAfterFive() {
        for (int i = 0; i < 4; i++) {
            assertThat(service.recordPasswordFailure("ali@ozerler.com", "10.0.1." + i))
                    .isEqualTo(LoginAttemptService.Outcome.FAILED);
        }
        assertThat(service.recordPasswordFailure("ali@ozerler.com", "10.0.1.9"))
                .isEqualTo(LoginAttemptService.Outcome.LOCKED);
        verify(userRepository).lockAccount(eq(7L), eq(LocalDateTime.now(clock).plusMinutes(15)));
    }

    @Test
    @DisplayName("Unknown user names count against the IP but lock nothing")
    void unknownUserOnlyCountsIp() {
        for (int i = 0; i < 5; i++) {
            assertThat(service.recordPasswordFailure("nobody@ozerler.com", "10.0.2.1"))
                    .isEqualTo(LoginAttemptService.Outcome.FAILED);
        }
        assertThat(service.isIpBlocked("10.0.2.1")).isTrue();
        verify(userRepository, never()).lockAccount(any(), any());
    }

    @Test
    @DisplayName("Wrong second-factor codes share the same account counter")
    void secondFactorCounts() {
        for (int i = 0; i < 4; i++) {
            service.recordSecondFactorFailure(7L, "10.0.3." + i);
        }
        assertThat(service.remainingAttempts(7L)).isEqualTo(1);
        assertThat(service.recordSecondFactorFailure(7L, "10.0.3.9"))
                .isEqualTo(LoginAttemptService.Outcome.LOCKED);
        verify(userRepository).lockAccount(eq(7L), any());
    }

    @Test
    @DisplayName("A full login resets the account counter")
    void resetClears() {
        service.resetAccount(7L);
        verify(userRepository).resetLoginFailures(7L);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
