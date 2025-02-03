package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerConverter;

public class ActiveTimerRepository {
    private ActiveTimerDao activeTimerDao;
    private LiveData<List<ActiveTimer>> allActiveTimersLiveData;
    private MutableLiveData<List<Timer>> timersLiveData;
    private Map<String, Timer> timersById;
    private Observer observer;

    public ActiveTimerRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);

        this.timersById = new HashMap<>();
        this.timersLiveData = new MutableLiveData<>(List.of());

        this.activeTimerDao = db.activeTimerDao();
        this.allActiveTimersLiveData = this.activeTimerDao.getAll();
        this.observer = (Observer<List<ActiveTimer>>) activeTimers -> {
            List<Timer> timers = activeTimers.stream()
                    .map(TimerConverter::fromActiveTimer)
                    .collect(Collectors.toList());
            ActiveTimerRepository.this.timersById.clear();
            timers.forEach(timer -> ActiveTimerRepository.this.timersById.put(timer.getId(), timer));
            ActiveTimerRepository.this.timersLiveData.setValue(timers);
        };

        this.allActiveTimersLiveData.observeForever(observer);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        this.allActiveTimersLiveData.removeObserver(observer);
    }

    public Timer getById(String id) {
        return timersById.get(id);
    }

    public LiveData<List<Timer>> getAllActiveTimers() {
        return this.timersLiveData;
    }

    public void insert(Timer timer) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.insert(TimerConverter.toActiveTimer(timer));
        });
    }

    public void update(Timer timer) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.update(TimerConverter.toActiveTimer(timer));
        });
    }

    public void deleteById(String id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.deleteById(id);
        });
    }
}
