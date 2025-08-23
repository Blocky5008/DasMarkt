package de.dasmarkt.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class TimeUtils {

    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Berlin");
    private static final int RESET_HOUR = 5;

    public static LocalDateTime getNextResetTime() {
        ZonedDateTime now = ZonedDateTime.now(TIMEZONE);
        ZonedDateTime nextReset = now.withHour(RESET_HOUR).withMinute(0).withSecond(0).withNano(0);
        if (now.isAfter(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }
        return nextReset.toLocalDateTime();
    }

    public static long toMillis(LocalDateTime time) {
        return time.atZone(TIMEZONE).toInstant().toEpochMilli();
    }
}