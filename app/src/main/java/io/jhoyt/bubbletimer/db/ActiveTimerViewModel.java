package io.jhoyt.bubbletimer.db;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.List;

import io.jhoyt.bubbletimer.Timer;

public class ActiveTimerViewModel extends AndroidViewModel {
    private final ActiveTimerRepository activeTimerRepository;
    private final LiveData<List<Timer>> allActiveTimers;

    public ActiveTimerViewModel(@NonNull Application application) {
        super(application);

        this.activeTimerRepository = new ActiveTimerRepository(application);
        this.allActiveTimers = activeTimerRepository.getAllActiveTimers();
    }

    public Timer getById(String id) {
        return activeTimerRepository.getById(id);
    }

    public LiveData<List<Timer>> getAllActiveTimers() {
        return allActiveTimers;
    }

    public void insert(Timer activeTimer) {
        activeTimerRepository.insert(activeTimer);
    }

    public void deleteById(String id) {
        activeTimerRepository.deleteById(id);
    }
}
