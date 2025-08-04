package io.jhoyt.bubbletimer.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity(tableName = "shared_timers")
public class SharedTimer {
    @PrimaryKey
    @NonNull
    public String timerId;
    
    @ColumnInfo(name = "name")
    @Nullable
    public String name;
    
    @ColumnInfo(name = "userId")
    @Nullable
    public String userId;
    
    @ColumnInfo(name = "totalDuration")
    @Nullable
    public Duration totalDuration;
    
    @ColumnInfo(name = "remainingDuration")
    @Nullable
    public Duration remainingDuration;
    
    @ColumnInfo(name = "timerEnd")
    @Nullable
    public LocalDateTime timerEnd;
    
    @ColumnInfo(name = "status") // PENDING, ACCEPTED, REJECTED
    @Nullable
    public String status;
    
    @ColumnInfo(name = "sharedBy")
    @Nullable
    public String sharedBy;
    
    @ColumnInfo(name = "createdAt")
    @Nullable
    public LocalDateTime createdAt;
} 