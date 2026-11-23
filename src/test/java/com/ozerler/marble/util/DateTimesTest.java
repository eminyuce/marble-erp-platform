package com.ozerler.marble.util;

import java.time.Duration;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DateTimesTest {

    @Test
    void formatsYearMonthDayHourMinuteWithFallback() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 11, 17, 50);
        Assertions.assertThat(DateTimes.formatYearMonthDayHourMinute(dateTime, "-")).isEqualTo("2026-09-11 17:50");
        Assertions.assertThat(DateTimes.formatYearMonthDayHourMinute(null, "-")).isEqualTo("-");
    }

    @Test
    void formatsTurkishLongDate() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 11, 17, 50);
        String formatted = DateTimes.formatTurkishLong(dateTime);
        Assertions.assertThat(formatted).contains("2026");
        Assertions.assertThat(formatted).contains("17:50");
        Assertions.assertThat(DateTimes.formatTurkishLong(null)).isEmpty();
    }

    @Test
    void formatsUptime() {
        Assertions.assertThat(DateTimes.formatUptime(Duration.ofHours(1).plusMinutes(2).plusSeconds(3)))
                .isEqualTo("1 sa 02 dk 03 sn");
        Assertions.assertThat(DateTimes.formatUptime(null)).isEqualTo("0 sa 00 dk 00 sn");
    }
}
