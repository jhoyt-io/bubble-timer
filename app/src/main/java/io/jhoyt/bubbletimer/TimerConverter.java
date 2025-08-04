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
        activeTimer.id = timerData.id != null ? timerData.id : "unknown";
        activeTimer.userId = timerData.userId != null ? timerData.userId : "unknown";
        activeTimer.name = timerData.name != null ? timerData.name : "Unknown Timer";
        activeTimer.totalDuration = timerData.totalDuration != null ? timerData.totalDuration : java.time.Duration.ZERO;
        activeTimer.remainingDurationWhenPaused = timerData.remainingDurationWhenPaused; // Can be null
        activeTimer.timerEnd = timerData.timerEnd; // Can be null
        Set<String> sharedWith = timer.getSharedWith();
        // Set to empty string when no shared users exist
        // This maintains compatibility with the database schema
        activeTimer.sharedWithString = (sharedWith == null || sharedWith.isEmpty()) ? "" : String.join("#~#", sharedWith);
        Set<String> tags = timer.getTags();
        activeTimer.tagsString = (tags == null || tags.isEmpty()) ? "" : String.join("#~#", tags);

        // Debug logging
        System.out.println("TimerConverter - ActiveTimer values:");
        System.out.println("  id: " + activeTimer.id);
        System.out.println("  userId: " + activeTimer.userId);
        System.out.println("  name: " + activeTimer.name);
        System.out.println("  totalDuration: " + activeTimer.totalDuration);
        System.out.println("  remainingDurationWhenPaused: " + activeTimer.remainingDurationWhenPaused);
        System.out.println("  timerEnd: " + activeTimer.timerEnd);
        System.out.println("  sharedWithString: '" + activeTimer.sharedWithString + "'");
        System.out.println("  tagsString: '" + activeTimer.tagsString + "'");

        return activeTimer;
    }
}
