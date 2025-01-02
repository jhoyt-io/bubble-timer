package io.jhoyt.bubbletimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashSet;
import java.util.Set;

public class ActiveTimerViewModel extends ViewModel {
    private final MutableLiveData<Set<Timer>> activeTimers = new MutableLiveData<Set<Timer>>();
    private final MutableLiveData<Timer> primaryTimer = new MutableLiveData<>();

    public void resetActiveTimers(Set<Timer> timers, Timer primaryTimer) {
        this.activeTimers.setValue(timers);
        this.primaryTimer.setValue(primaryTimer);
    }

    public void activateTimer(Timer timer) {
        Set<Timer> current = activeTimers.getValue();
        if (current == null) {
            current = new HashSet<>();
        }
        current.add(timer);
        activeTimers.setValue(current);

        if (primaryTimer.getValue() == null) {
            primaryTimer.setValue(timer);
        }
    }

    public void deactivateTimer(Timer timer) {
        Set<Timer> current = activeTimers.getValue();
        if (current == null || timer == null) {
            return;
        }
        current.remove(timer);
        activeTimers.setValue(current);

        if (primaryTimer.getValue() != null &&
                primaryTimer.getValue().getId().equals(timer.getId())) {
            if (!activeTimers.getValue().isEmpty()) {
                primaryTimer.setValue((Timer)activeTimers.getValue().toArray()[0]);
            } else {
                primaryTimer.setValue(null);
            }
        }
    }

    public void setPrimaryTimer(Timer timer) {
        primaryTimer.setValue(timer);
    }

    public LiveData<Timer> getPrimaryTimer() {
        return primaryTimer;
    }

    public LiveData<Set<Timer>> getActiveTimers() {
        return activeTimers;
    }



}
