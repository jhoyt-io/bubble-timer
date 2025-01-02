package io.jhoyt.bubbletimer;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimerData {
    public final String id;
    public final String userId;
    public final String name;
    public final Duration totalDuration;
    public final Duration remainingDurationWhenPaused;
    public final LocalDateTime timerEnd;

    public TimerData(String id, String userId, String name, Duration totalDuration, Duration remainingDurationWhenPaused, LocalDateTime timerEnd) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.totalDuration = totalDuration;
        this.remainingDurationWhenPaused = remainingDurationWhenPaused;
        this.timerEnd = timerEnd;
    }
}
