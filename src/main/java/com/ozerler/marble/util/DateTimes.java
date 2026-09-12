package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class DateTimes {

    private static final DateTimeFormatter TURKISH_LONG =
            DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT_TURKISH_LONG, Constants.LOCALE_TR);
    private static final DateTimeFormatter YEAR_MONTH_DAY_HOUR_MINUTE =
            DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT_YMD_HM);

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
            return Constants.DEFAULT_UPTIME_FORMAT;
        }
        return String.format("%d sa %02d dk %02d sn",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());
    }
}
