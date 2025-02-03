package io.jhoyt.bubbletimer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimerData)) return false;
        TimerData timerData = (TimerData) o;
        return Objects.equals(id, timerData.id) && Objects.equals(userId, timerData.userId) && Objects.equals(name, timerData.name) && Objects.equals(totalDuration, timerData.totalDuration) && Objects.equals(remainingDurationWhenPaused, timerData.remainingDurationWhenPaused) && Objects.equals(timerEnd, timerData.timerEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, name, totalDuration, remainingDurationWhenPaused, timerEnd);
    }
}
