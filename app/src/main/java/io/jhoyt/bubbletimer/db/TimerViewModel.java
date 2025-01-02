package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class TimerViewModel extends AndroidViewModel {
    private TimerRepository timerRepository;

    private final LiveData<List<Timer>> allTimers;

    public TimerViewModel(@NonNull Application application) {
        super(application);

        timerRepository = new TimerRepository(application);
        allTimers = timerRepository.getAllTimers();
    }

    public LiveData<List<Timer>> getAllTimers() {
        return allTimers;
    }

    public void insert(Timer timer) {
        timerRepository.insert(timer);
    }

    public void deleteById(int timerId) {
        timerRepository.deleteById(timerId);
    }
}
