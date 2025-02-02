package io.jhoyt.bubbletimer;

import io.jhoyt.bubbletimer.db.ActiveTimer;

public class TimerConverter {
    public static Timer fromActiveTimer(ActiveTimer activeTimer) {
        return new Timer(new TimerData(
                activeTimer.id,
                activeTimer.userId,
                activeTimer.name,
                activeTimer.totalDuration,
                activeTimer.remainingDurationWhenPaused,
                activeTimer.timerEnd
        ));
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

        return activeTimer;
    }
}
