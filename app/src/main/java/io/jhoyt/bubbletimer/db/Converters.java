package io.jhoyt.bubbletimer.db;

import androidx.room.TypeConverter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Converters {
    @TypeConverter
    public static Duration fromSeconds(Long value) {
        return value == null ? null : Duration.ofSeconds(value);
    }

    @TypeConverter
    public static Long durationToSeconds(Duration duration) {
        return duration == null ? null : duration.getSeconds();
    }

    @TypeConverter
    public static LocalDateTime fromString(String value) {
        return (value == null || value.isEmpty()) ? null : LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @TypeConverter
    public static String localDateTimeToString(LocalDateTime localDateTime) {
        return localDateTime == null ? "" : localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
