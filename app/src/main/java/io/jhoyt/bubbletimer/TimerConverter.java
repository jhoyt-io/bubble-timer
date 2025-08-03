package io.jhoyt.bubbletimer;

import java.util.HashSet;
import java.util.Set;

import io.jhoyt.bubbletimer.db.ActiveTimer;

public class TimerConverter {
    public static Timer fromActiveTimer(ActiveTimer activeTimer) {
        return new Timer(new TimerData(
                activeTimer.id,
                activeTimer.userId,
                activeTimer.name,
                activeTimer.totalDuration,
                activeTimer.remainingDurationWhenPaused,
                activeTimer.timerEnd,
                activeTimer.tagsString == null ?
                        Set.of()
                        : Set.of(activeTimer.tagsString.split("#~#"))
        ), activeTimer.sharedWithString == null || activeTimer.sharedWithString.isEmpty() ?
                Set.of()
                : Set.of(activeTimer.sharedWithString.split("#~#")).stream()
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    public static ActiveTimer toActiveTimer(Timer timer) {
        TimerData timerData = timer.getTimerData();

        ActiveTimer activeTimer = new ActiveTimer();
        activeTimer.id = timerData.id;
        activeTimer.userId = timerData.userId;
        activeTimer.name = timerData.name;
        activeTimer.totalDuration = timerData.totalDuration;
        activeTimer.remainingDurationWhenPaused = timerData.remainingDurationWhenPaused;
        activeTimer.timerEnd = timerData.timerEnd;
        Set<String> sharedWith = timer.getSharedWith();
        // Set to empty string when no shared users exist
        // This maintains compatibility with the database schema
        activeTimer.sharedWithString = sharedWith.isEmpty() ? "" : String.join("#~#", sharedWith);
        activeTimer.tagsString = String.join("#~#", timer.getTags());

        return activeTimer;
    }
}
