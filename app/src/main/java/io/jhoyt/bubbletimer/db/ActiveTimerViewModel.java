package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import io.jhoyt.bubbletimer.Timer;

public class ActiveTimerViewModel extends AndroidViewModel {
    private final ActiveTimerRepository activeTimerRepository;

    private final LiveData<List<ActiveTimer>> allActiveTimers;

    public ActiveTimerViewModel(@NonNull Application application) {
        super(application);

        activeTimerRepository = null;
        allActiveTimers = null;
    }

    public Timer getById(String id) {
        return activeTimerRepository.getById(id);
    }

    public LiveData<List<ActiveTimer>> getAllActiveTimers() {
        return allActiveTimers;
    }

    public void insert(Timer activeTimer) {
        activeTimerRepository.insert(activeTimer);
    }

    public void deleteById(String id) {
        activeTimerRepository.deleteById(id);
    }
}
