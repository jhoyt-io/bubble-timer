package io.jhoyt.bubbletimer.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
public class ActiveTimer {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "userId")
    public String userId;

    @ColumnInfo(name = "totalDuration")
    public Duration totalDuration;

    @ColumnInfo(name = "remainingDurationWhenPaused")
    public Duration remainingDurationWhenPaused;

    @ColumnInfo(name = "timerEnd")
    public LocalDateTime timerEnd;
}
