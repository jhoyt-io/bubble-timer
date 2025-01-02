package io.jhoyt.bubbletimer.db;

import androidx.room.TypeConverter;

import java.time.Duration;

public class Converters {
    @TypeConverter
    public static Duration fromSeconds(Long value) {
        return value == null ? null : Duration.ofSeconds(value);
    }

    @TypeConverter
    public static Long durationToSeconds(Duration duration) {
        return duration == null ? null : duration.getSeconds();
    }
}
