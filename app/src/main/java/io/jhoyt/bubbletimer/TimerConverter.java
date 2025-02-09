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
        ), activeTimer.sharedWithString == null ?
                Set.of()
                : Set.of(activeTimer.sharedWithString.split("#~#")));
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
        activeTimer.sharedWithString = String.join("#~#", timer.getSharedWith());
        activeTimer.tagsString = String.join("#~#", timer.getTags());

        return activeTimer;
    }
}
