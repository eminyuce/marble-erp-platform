package com.ozerler.marble.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimesTest {

    @Test
    void formatsYearMonthDayHourMinuteWithFallback() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 11, 17, 50);
        assertThat(DateTimes.formatYearMonthDayHourMinute(dateTime, "-")).isEqualTo("2026-09-11 17:50");
        assertThat(DateTimes.formatYearMonthDayHourMinute(null, "-")).isEqualTo("-");
    }

    @Test
    void formatsTurkishLongDate() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 11, 17, 50);
        String formatted = DateTimes.formatTurkishLong(dateTime);
        assertThat(formatted).contains("2026");
        assertThat(formatted).contains("17:50");
        assertThat(DateTimes.formatTurkishLong(null)).isEmpty();
    }

    @Test
    void formatsUptime() {
        assertThat(DateTimes.formatUptime(Duration.ofHours(1).plusMinutes(2).plusSeconds(3)))
                .isEqualTo("1 sa 02 dk 03 sn");
        assertThat(DateTimes.formatUptime(null)).isEqualTo("0 sa 00 dk 00 sn");
    }
}
