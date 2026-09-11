package com.ozerler.marble.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public final class DateTimes {

    private static final DateTimeFormatter TURKISH_LONG =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy HH:mm", Locale.forLanguageTag("tr"));
    private static final DateTimeFormatter YEAR_MONTH_DAY_HOUR_MINUTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateTimes() {
    }

    public static String formatTurkishLong(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TURKISH_LONG);
    }

    public static String formatYearMonthDayHourMinute(TemporalAccessor dateTime, String fallback) {
        if (dateTime == null) {
            return fallback == null ? "" : fallback;
        }
        return YEAR_MONTH_DAY_HOUR_MINUTE.format(dateTime);
    }

    public static String formatUptime(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "0 sa 00 dk 00 sn";
        }
        return String.format("%d sa %02d dk %02d sn",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());
    }
}
